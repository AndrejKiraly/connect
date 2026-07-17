package com.andrejKir.connect.social.service;


import com.andrejKir.connect.shared.domain.UserPair;
import com.andrejKir.connect.social.dto.FriendshipRequest;
import com.andrejKir.connect.social.dto.FriendshipResponse;
import com.andrejKir.connect.social.entity.Friendship;
import com.andrejKir.connect.social.exception.AlreadyFriendsException;
import com.andrejKir.connect.social.exception.FriendshipRequestAlreadyPendingException;
import com.andrejKir.connect.social.exception.FriendshipRequestOnCooldownException;
import com.andrejKir.connect.social.exception.SelfFriendshipException;
import com.andrejKir.connect.social.repository.FriendshipRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;

    public FriendshipService(FriendshipRepository friendshipRepository){
        this.friendshipRepository = friendshipRepository;
    }

    public FriendshipResponse createFriendshipRequest(FriendshipRequest friendshipRequest) {
        if (friendshipRequest.requesterId().equals(friendshipRequest.targetId())) {
            throw new SelfFriendshipException();
        }
        UserPair userPair = UserPair.of(friendshipRequest.requesterId(),friendshipRequest.targetId());

        friendshipRepository.findByUsers(userPair).ifPresent(existing -> {
            throw switch (existing.getStatus()) {
                case ACCEPTED -> new AlreadyFriendsException();
                case PENDING  -> new FriendshipRequestAlreadyPendingException();
                case DECLINED -> new FriendshipRequestOnCooldownException();
            };
        });


        Friendship saved = friendshipRepository.save(
                Friendship.request(userPair, friendshipRequest.requesterId()));     // pair znova nepočíta
        return FriendshipResponse.from(saved);
    }

    public FriendshipResponse confirmFriendshipRequest(UUID friendshipRequestId){
        throw new SelfFriendshipException();
    }
}
