package com.andrejKir.connect.messaging.dto.response;

import java.util.List;
import java.util.UUID;

public record ConversationPageResponse(
        List<ConversationSummaryResponse> conversations,
        UUID nextCursor
) {
}
