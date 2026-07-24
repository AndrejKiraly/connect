package com.andrejKir.connect.accounts.dto.response;

import com.andrejKir.connect.accounts.entity.AppUser;

import java.util.UUID;

public record AppUserPrivateSummaryResponse(
        UUID id,
        String username,
        String displayName,
        String description
) {
    public static AppUserPrivateSummaryResponse from(AppUser user) {
        return new AppUserPrivateSummaryResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getDescription());
    }
}