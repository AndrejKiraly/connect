package com.andrejKir.connect.accounts.dto.response;

import com.andrejKir.connect.accounts.entity.AppUser;

public record AppUserPublicDetailResponse(
        AppUserPublicSummaryResponse user,
        String description
) {
    public static AppUserPublicDetailResponse from(AppUser appUser){
        AppUserPublicSummaryResponse user = AppUserPublicSummaryResponse.from(appUser);
        return new AppUserPublicDetailResponse(user, user.deleted() ? "" : appUser.getDescription());
    }
}