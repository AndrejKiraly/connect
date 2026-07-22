package com.andrejKir.connect.social.service;


import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.shared.domain.UserPair;
import com.andrejKir.connect.social.dto.FriendshipRequest;
import com.andrejKir.connect.social.dto.FriendshipResponse;
import com.andrejKir.connect.social.entity.Friendship;
import com.andrejKir.connect.social.enums.FriendshipStatus;
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

import java.util.UUID;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final AppUserService appUserService;

    public FriendshipService(FriendshipRepository friendshipRepository, AppUserService appUserService) {
        this.friendshipRepository = friendshipRepository;
        this.appUserService = appUserService;
    }

    @Transactional
    public FriendshipResponse createFriendshipRequest(UUID actorId, FriendshipRequest request) {
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
            return FriendshipResponse.from(saved);
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
        return FriendshipResponse.from(friendship);
    }

    @Transactional
    public FriendshipResponse declineFriendshipRequest(UUID actorId, UUID friendshipRequestId) {
        Friendship friendship = requireAddressee(actorId, friendshipRequestId);
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new FriendshipNotPendingException();
        }
        friendship.decline();
        return FriendshipResponse.from(friendship);
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
