package com.andrejKir.connect.social.dto;
import com.andrejKir.connect.accounts.dto.response.AppUserPublicSummaryResponse;
import com.andrejKir.connect.social.entity.Friendship;
import com.andrejKir.connect.social.enums.FriendshipStatus;


import java.time.Instant;
import java.util.UUID;

public record FriendshipResponse (
        UUID id,
        FriendshipStatus status,
        AppUserPublicSummaryResponse counterpart,
        Instant createdAt)
    {
        public static FriendshipResponse from(Friendship friendship, AppUserPublicSummaryResponse
                counterpart) {
            return new FriendshipResponse(friendship.getId(), friendship.getStatus(), counterpart,
                    friendship.getCreatedAt());
        }
    }
