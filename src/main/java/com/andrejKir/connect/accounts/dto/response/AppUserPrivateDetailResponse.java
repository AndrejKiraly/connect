package com.andrejKir.connect.accounts.dto.response;

import com.andrejKir.connect.accounts.entity.AppUser;

import java.time.LocalDate;
import java.util.UUID;

public record AppUserPrivateDetailResponse(
        UUID id,
        String username,
        String email,
        String displayName,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String description
) {
    public static AppUserPrivateDetailResponse from(AppUser user) {
        return new AppUserPrivateDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getFirstName(),
                user.getLastName(),
                user.getBirthDate(),
                user.getDescription());
    }
}