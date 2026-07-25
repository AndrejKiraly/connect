package com.andrejKir.connect.social.service;

import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.shared.domain.UserPair;
import com.andrejKir.connect.shared.ratelimit.RateLimitPolicy;
import com.andrejKir.connect.shared.ratelimit.RateLimitService;
import com.andrejKir.connect.social.dto.FriendshipRequest;
import com.andrejKir.connect.social.entity.Friendship;
import com.andrejKir.connect.social.enums.FriendshipStatus;
import com.andrejKir.connect.social.exception.AlreadyFriendsException;
import com.andrejKir.connect.social.exception.FriendshipRequestAlreadyPendingException;
import com.andrejKir.connect.social.exception.FriendshipRequestLimitExceededException;
import com.andrejKir.connect.social.exception.FriendshipRequestOnCooldownException;
import com.andrejKir.connect.social.exception.SelfFriendshipException;
import com.andrejKir.connect.social.repository.FriendshipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    private static final long PENDING_LIMIT = 100L;

    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private AppUserService appUserService;
    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private FriendshipService friendshipService;

    private final UUID actorId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @Test
    void createFriendshipRequest_consumesRateLimitBeforeValidation() {
        assertThrows(SelfFriendshipException.class, () -> createRequestTo(actorId));

        verify(rateLimitService).check(RateLimitPolicy.FRIENDSHIP_REQUEST_PER_USER, actorId.toString());
    }

    @ParameterizedTest
    @MethodSource("existingRelations")
    void createFriendshipRequest_existingRelation_isRejected(FriendshipStatus status,
                                                             Class<? extends RuntimeException> expected) {
        stubTargetExists();
        when(friendshipRepository.findByUsers(any())).thenReturn(Optional.of(relationIn(status)));

        assertThrows(expected, () -> createRequestTo(targetId));

        verify(friendshipRepository, never()).saveAndFlush(any());
    }

    @Test
    void createFriendshipRequest_atPendingLimit_isRejected() {
        stubTargetIsNewCounterpart();
        when(friendshipRepository.countPendingRequestsRequestedByUser(actorId)).thenReturn(PENDING_LIMIT);

        assertThrows(FriendshipRequestLimitExceededException.class, () -> createRequestTo(targetId));

        verify(friendshipRepository, never()).saveAndFlush(any());
    }

    @Test
    void createFriendshipRequest_belowPendingLimit_isPersisted() {
        stubTargetIsNewCounterpart();
        when(friendshipRepository.countPendingRequestsRequestedByUser(actorId)).thenReturn(PENDING_LIMIT - 1);
        when(friendshipRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        createRequestTo(targetId);

        verify(friendshipRepository).saveAndFlush(any());
    }

    @Test
    void createFriendshipRequest_losingConcurrentInsert_isRejectedAsAlreadyPending() {
        stubTargetIsNewCounterpart();
        when(friendshipRepository.countPendingRequestsRequestedByUser(actorId)).thenReturn(0L);
        when(friendshipRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uq_friendship"));

        assertThrows(FriendshipRequestAlreadyPendingException.class, () -> createRequestTo(targetId));
    }

    private static Stream<Arguments> existingRelations() {
        return Stream.of(
                Arguments.of(FriendshipStatus.PENDING, FriendshipRequestAlreadyPendingException.class),
                Arguments.of(FriendshipStatus.ACCEPTED, AlreadyFriendsException.class),
                Arguments.of(FriendshipStatus.DECLINED, FriendshipRequestOnCooldownException.class));
    }

    private void createRequestTo(UUID target) {
        friendshipService.createFriendshipRequest(actorId, new FriendshipRequest(target));
    }

    private void stubTargetIsNewCounterpart() {
        stubTargetExists();
        when(friendshipRepository.findByUsers(any())).thenReturn(Optional.empty());
    }

    private void stubTargetExists() {
        when(appUserService.exists(targetId)).thenReturn(true);
    }

    private Friendship relationIn(FriendshipStatus status) {
        Friendship friendship = Friendship.request(UserPair.of(actorId, targetId), actorId);
        switch (status) {
            case ACCEPTED -> friendship.accept();
            case DECLINED -> friendship.decline();
            case PENDING -> { }
        }
        return friendship;
    }
}