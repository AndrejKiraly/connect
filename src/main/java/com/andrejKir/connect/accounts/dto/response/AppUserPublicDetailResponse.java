package com.andrejKir.connect.accounts.dto.response;

import com.andrejKir.connect.accounts.entity.AppUser;

import java.time.LocalDate;
import java.util.UUID;

public record AppUserPublicDetailResponse(
        String displayName,
        String description
) {
    public static AppUserPublicDetailResponse from(AppUser appUser){
        return  new AppUserPublicDetailResponse(appUser.getDisplayName(), appUser.getDescription());
    }
}
