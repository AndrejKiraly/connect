package com.andrejKir.connect.messaging.dto.response;

import java.util.List;
import java.util.UUID;

public record ConversationPageResponse(
        List<ConversationSummaryResponse> conversations,
        UUID nextCursor,
        boolean hasMore
) {
    public static ConversationPageResponse of(List<ConversationSummaryResponse> conversations, boolean hasMore) {
        return new ConversationPageResponse(
                conversations,
                hasMore ? conversations.getLast().lastMessage().id() : null,
                hasMore);
    }
}
