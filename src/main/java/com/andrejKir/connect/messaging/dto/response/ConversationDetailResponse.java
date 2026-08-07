package com.andrejKir.connect.messaging.dto.response;

import com.andrejKir.connect.accounts.dto.response.AppUserPublicSummaryResponse;
import com.andrejKir.connect.messaging.entity.Conversation;
import com.andrejKir.connect.messaging.enums.ConversationType;

import java.time.Instant;
import java.util.UUID;

public record ConversationDetailResponse(
        UUID id,
        ConversationType type,
        AppUserPublicSummaryResponse counterpart,
        Instant createdAt
) {
    public static ConversationDetailResponse from(Conversation conversation, AppUserPublicSummaryResponse counterpart) {
        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getType(),
                counterpart,
                conversation.getCreatedAt());
    }
}
