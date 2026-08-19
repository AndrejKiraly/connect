package com.andrejKir.connect.accounts.service;

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
}