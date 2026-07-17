package com.andrejKir.connect.social.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FriendshipRequest (
        @NotNull UUID requesterId, @NotNull UUID targetId
)
{
}
