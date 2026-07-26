package com.andrejKir.connect.accounts.dto.response;

import com.andrejKir.connect.accounts.entity.AppUser;

import java.util.UUID;

public record AppUserPublicDetailResponse(
        UUID id,
        String displayName,
        String description
) {
    private static final String DELETED_DISPLAY_NAME = "Deleted user";

    public static AppUserPublicDetailResponse from(AppUser appUser){
        if (appUser.isDeactivated()) {
            return new AppUserPublicDetailResponse(appUser.getId(), DELETED_DISPLAY_NAME, "");
        }
        return new AppUserPublicDetailResponse(appUser.getId(), appUser.getDisplayName(), appUser.getDescription());
    }
}