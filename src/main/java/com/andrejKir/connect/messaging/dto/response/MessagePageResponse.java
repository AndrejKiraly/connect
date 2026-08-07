package com.andrejKir.connect.messaging.dto.response;


import com.andrejKir.connect.accounts.dto.response.AppUserPublicSummaryResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MessagePageResponse(
        List<MessageResponse> messages,
        Map<UUID, AppUserPublicSummaryResponse> users,
        UUID nextCursor,
        boolean hasMore
) {
}
