package com.andrejKir.connect.messaging.dto.response;

import com.andrejKir.connect.accounts.dto.response.AppUserPublicSummaryResponse;
import com.andrejKir.connect.messaging.enums.ConversationType;
import com.andrejKir.connect.messaging.repository.ConversationInboxRow;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ConversationSummaryResponse(
        UUID id,
        ConversationType type,
        AppUserPublicSummaryResponse counterpart,
        LastMessageResponse lastMessage,
        boolean unread,
        Instant lastActivityAt
) {
    public static ConversationSummaryResponse from(ConversationInboxRow row,
                                                   Map<UUID, AppUserPublicSummaryResponse> users,
                                                   UUID actorId) {
        UUID counterpartId = row.getCounterpartId();
        return new ConversationSummaryResponse(
                row.getId(),
                ConversationType.valueOf(row.getType()),
                counterpartId == null ? null : users.get(counterpartId),
                LastMessageResponse.from(row, users, actorId),
                row.isUnread(),
                row.getLastMessageAt());
    }
}