package com.andrejKir.connect.accounts.dto.response;


import com.andrejKir.connect.accounts.entity.AppUser;

import java.util.UUID;

public record AppUserResponse (
    UUID id,
    String username,
    String displayName
) {
    public static AppUserResponse from(AppUser user)
    {
        return new AppUserResponse(user.getId(), user.getUsername(), user.getDisplayName());
    }
}
