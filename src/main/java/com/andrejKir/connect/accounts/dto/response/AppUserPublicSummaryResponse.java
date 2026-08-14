package com.andrejKir.connect.accounts.dto.response;

import com.andrejKir.connect.accounts.entity.AppUser;

import java.util.UUID;

public record AppUserPublicSummaryResponse(
        UUID id,
        String displayName,
        boolean deleted
) {
    private static final String DELETED_DISPLAY_NAME = "Deleted user";

    public static AppUserPublicSummaryResponse from(AppUser user) {
        if (user.isDeactivated()) {
            return new AppUserPublicSummaryResponse(user.getId(), DELETED_DISPLAY_NAME, true);
        }
        return new AppUserPublicSummaryResponse(user.getId(), user.getDisplayName(), false);
    }
}