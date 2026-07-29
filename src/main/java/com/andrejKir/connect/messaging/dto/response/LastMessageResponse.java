package com.andrejKir.connect.messaging.dto.response;

import com.andrejKir.connect.accounts.dto.response.AppUserPublicSummaryResponse;
import com.andrejKir.connect.messaging.enums.MessageType;
import com.andrejKir.connect.messaging.repository.ConversationInboxRow;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record LastMessageResponse(
        UUID id,
        MessageType type,
        AppUserPublicSummaryResponse sender,
        boolean sentByMe,
        String preview,
        boolean truncated,
        Instant createdAt
) {
    public static LastMessageResponse from(ConversationInboxRow row,
                                           Map<UUID, AppUserPublicSummaryResponse> senders,
                                           UUID actorId) {
        UUID senderId = row.getLastMessageSenderId();
        return new LastMessageResponse(
                row.getLastMessageId(),
                MessageType.valueOf(row.getLastMessageType()),
                senders.get(senderId),
                senderId.equals(actorId),
                row.getPreview(),
                row.isTruncated(),
                row.getLastMessageAt());
    }
}