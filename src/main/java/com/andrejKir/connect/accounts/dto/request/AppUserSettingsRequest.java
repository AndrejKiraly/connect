package com.andrejKir.connect.accounts.dto.request;

import com.andrejKir.connect.accounts.enums.PostLifespan;
import com.andrejKir.connect.accounts.enums.SupportedLanguage;
import jakarta.validation.constraints.NotNull;

public record AppUserSettingsRequest(
        @NotNull SupportedLanguage language,
        @NotNull PostLifespan defaultPostLifespan,
        @NotNull Boolean discoverableByName,
        @NotNull Boolean discoverableByCode
) {
}