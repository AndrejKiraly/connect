package com.andrejKir.connect.social.service;


import com.andrejKir.connect.accounts.dto.response.AppUserPublicSummaryResponse;
import com.andrejKir.connect.accounts.entity.AppUser;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.shared.domain.UserPair;
import com.andrejKir.connect.shared.ratelimit.RateLimitPolicy;
import com.andrejKir.connect.shared.ratelimit.RateLimitService;
import com.andrejKir.connect.social.dto.FriendshipRequest;
import com.andrejKir.connect.social.dto.FriendshipResponse;
import com.andrejKir.connect.social.entity.Friendship;
import com.andrejKir.connect.social.enums.FriendshipStatus;
import com.andrejKir.connect.social.enums.RequestDirection;
import com.andrejKir.connect.social.exception.AlreadyFriendsException;
import com.andrejKir.connect.social.exception.FriendshipNotPendingException;
import com.andrejKir.connect.social.exception.FriendshipRequestAlreadyPendingException;
import com.andrejKir.connect.social.exception.FriendshipRequestNotFoundException;
import com.andrejKir.connect.social.exception.FriendshipRequestOnCooldownException;
import com.andrejKir.connect.social.exception.FriendshipTargetNotFoundException;
import com.andrejKir.connect.social.exception.OwnFriendshipRequestException;
import com.andrejKir.connect.social.exception.SelfFriendshipException;
import com.andrejKir.connect.social.repository.FriendshipRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final AppUserService appUserService;
    private final RateLimitService rateLimitService;


    public FriendshipService(FriendshipRepository friendshipRepository, AppUserService appUserService, RateLimitService rateLimitService) {
        this.friendshipRepository = friendshipRepository;
        this.appUserService = appUserService;
        this.rateLimitService = rateLimitService;
    }

    @Transactional
    public FriendshipResponse createFriendshipRequest(UUID actorId, FriendshipRequest request) {
        rateLimitService.check(RateLimitPolicy.FRIENDSHIP_REQUEST_PER_USER, actorId.toString());
        if (actorId.equals(request.targetId())) {
            throw new SelfFriendshipException();
        }
        if (!appUserService.exists(request.targetId())) {
            throw new FriendshipTargetNotFoundException();
        }

        UserPair userPair = UserPair.of(actorId, request.targetId());
        friendshipRepository.findByUsers(userPair).ifPresent(existing -> {
            throw switch (existing.getStatus()) {
                case ACCEPTED -> new AlreadyFriendsException();
                case PENDING  -> new FriendshipRequestAlreadyPendingException();
                case DECLINED -> new FriendshipRequestOnCooldownException();
            };
        });

        try {
            Friendship saved = friendshipRepository.saveAndFlush(Friendship.request(userPair, actorId));
            return toResponse(saved, actorId);
        } catch (DataIntegrityViolationException e) {
            throw new FriendshipRequestAlreadyPendingException();
        }
    }

    @Transactional
    public FriendshipResponse confirmFriendshipRequest(UUID actorId, UUID friendshipRequestId) {
        Friendship friendship = requireAddressee(actorId, friendshipRequestId);
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new FriendshipNotPendingException();
        }
        friendship.accept();
        return toResponse(friendship, actorId);
    }

    @Transactional
    public FriendshipResponse declineFriendshipRequest(UUID actorId, UUID friendshipRequestId) {
        Friendship friendship = requireAddressee(actorId, friendshipRequestId);
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new FriendshipNotPendingException();
        }
        friendship.decline();
        return toResponse(friendship, actorId);
    }

    @Transactional(readOnly = true)
    public List<FriendshipResponse> listFriends(UUID actorId) {
        return toResponses(friendshipRepository.findAcceptedRequestsByUser(actorId), actorId);
    }

    @Transactional(readOnly = true)
    public List<FriendshipResponse> listRequests(UUID actorId, RequestDirection direction) {
        List<Friendship> rows = switch (direction) {
            case INCOMING -> friendshipRepository.findPendingRequestsRequestedToUser(actorId);
            case OUTGOING -> friendshipRepository.findPendingRequestsRequestedByUser(actorId);
        };
        return toResponses(rows, actorId);
    }

    private List<FriendshipResponse> toResponses(List<Friendship> rows, UUID actorId) {
        Set<UUID> counterpartIds = rows.stream()
                .map(f -> f.counterpartOf(actorId))
                .collect(Collectors.toSet());
        Map<UUID, AppUserPublicSummaryResponse> summaries = appUserService.getSummaries(counterpartIds);
        return rows.stream()
                .map(f -> FriendshipResponse.from(f, summaries.get(f.counterpartOf(actorId))))
                .toList();
    }

    private FriendshipResponse toResponse(Friendship friendship, UUID actorId) {
        AppUserPublicSummaryResponse counterpart = appUserService.getSummary(friendship.counterpartOf(actorId));
        return FriendshipResponse.from(friendship, counterpart);
    }

    private Friendship requireAddressee(UUID actorId, UUID friendshipRequestId) {
        Friendship friendship = friendshipRepository.findById(friendshipRequestId)
                .orElseThrow(FriendshipRequestNotFoundException::new);
        if (!friendship.involves(actorId)) {
            throw new FriendshipRequestNotFoundException();
        }
        if (friendship.isRequestedBy(actorId)) {
            throw new OwnFriendshipRequestException();
        }
        return friendship;
    }
}
