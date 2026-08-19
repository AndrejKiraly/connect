package com.andrejKir.connect.accounts.controller;

import com.andrejKir.connect.accounts.dto.request.AppUserSettingsRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserSettingsResponse;
import com.andrejKir.connect.accounts.security.SecurityUser;
import com.andrejKir.connect.accounts.service.AppUserSettingsService;
import com.andrejKir.connect.shared.web.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(ApiPaths.V1 + "/users/me/settings")
public class AppUserSettingsController {

    private final AppUserSettingsService appUserSettingsService;

    public AppUserSettingsController(AppUserSettingsService appUserSettingsService) {
        this.appUserSettingsService = appUserSettingsService;
    }

    @GetMapping
    public AppUserSettingsResponse showSettings(@AuthenticationPrincipal SecurityUser principal){
        AppUserSettingsResponse appUserSettingsResponse = appUserSettingsService.showSettings(principal.getId());
        return appUserSettingsResponse;
    }

    @PutMapping
    public AppUserSettingsResponse changeSettings(@Valid @RequestBody AppUserSettingsRequest appUserSettingsRequest, @AuthenticationPrincipal SecurityUser principal){
        AppUserSettingsResponse appUserSettingsResponse = appUserSettingsService.changeSettings(appUserSettingsRequest, principal.getId());
        return appUserSettingsResponse;
    }
}
