package com.andrejKir.connect.social.dto;
import com.andrejKir.connect.social.entity.Friendship;
import com.andrejKir.connect.social.enums.FriendshipStatus;
import jakarta.validation.constraints.NotBlank;


import java.time.Instant;
import java.util.UUID;

public record FriendshipResponse (
     FriendshipStatus status,
     UUID userHighId,
     UUID userLowId,
     UUID requestedBy,
     Instant createdAt)
    {
        public static FriendshipResponse from(Friendship friendship)
        {
            return new FriendshipResponse(
                    friendship.getStatus(),
                    friendship.getUserHighId(),
                    friendship.getUserLowId(),
                    friendship.getRequestedBy(),
                    friendship.getCreatedAt()
            );
        }
    }
