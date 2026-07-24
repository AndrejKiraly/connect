package com.andrejKir.connect.accounts.dto.response;

import com.andrejKir.connect.accounts.entity.AppUser;

import java.util.UUID;

public record AppUserPublicSummaryResponse(
        UUID id,
        String displayName
) {
    public static AppUserPublicSummaryResponse from(AppUser user) {
        return new AppUserPublicSummaryResponse(user.getId(), user.getDisplayName());
    }
}