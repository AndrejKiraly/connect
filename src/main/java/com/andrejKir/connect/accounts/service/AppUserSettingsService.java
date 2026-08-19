package com.andrejKir.connect.accounts.service;

import com.andrejKir.connect.accounts.dto.request.AppUserSettingsRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserSettingsResponse;
import com.andrejKir.connect.accounts.entity.AppUserSettings;
import com.andrejKir.connect.accounts.repository.AppUserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AppUserSettingsService {

    private final AppUserSettingsRepository appUserSettingsRepository;

    public AppUserSettingsService(AppUserSettingsRepository appUserSettingsRepository) {
        this.appUserSettingsRepository = appUserSettingsRepository;
    }

    @Transactional
    public void createDefaults(UUID appUserId) {
        appUserSettingsRepository.save(new AppUserSettings(appUserId));
    }

    @Transactional(readOnly = true)
    public AppUserSettingsResponse showSettings(UUID principalId) {
        return AppUserSettingsResponse.from(requireSettings(principalId));
    }

    @Transactional
    public AppUserSettingsResponse changeSettings(AppUserSettingsRequest request, UUID principalId) {
        AppUserSettings appUserSettings = requireSettings(principalId);
        appUserSettings.update(request.language(), request.defaultPostLifespan(),
                request.discoverableByName(), request.discoverableByCode());
        return AppUserSettingsResponse.from(appUserSettings);
    }

    private AppUserSettings requireSettings(UUID principalId) {
        return appUserSettingsRepository.findById(principalId).orElseThrow(
                () -> new IllegalStateException("Settings not found for user: " + principalId));
    }
}