package com.andrejKir.connect.messaging.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateConversationRequest(
        @NotNull UUID counterpartId
) {
}
