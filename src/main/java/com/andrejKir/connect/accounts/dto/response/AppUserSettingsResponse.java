package com.andrejKir.connect.accounts.dto.response;

import com.andrejKir.connect.accounts.entity.AppUserSettings;
import com.andrejKir.connect.accounts.enums.PostLifespan;
import com.andrejKir.connect.accounts.enums.SupportedLanguage;

public record AppUserSettingsResponse(
        SupportedLanguage language,
        PostLifespan defaultPostLifespan,
        boolean discoverableByName,
        boolean discoverableByCode
) {
    public static AppUserSettingsResponse from(AppUserSettings settings) {
        return new AppUserSettingsResponse(
                settings.getLanguage(),
                settings.getDefaultPostLifespan(),
                settings.isDiscoverableByName(),
                settings.isDiscoverableByCode());
    }
}