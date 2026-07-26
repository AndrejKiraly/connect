package com.andrejKir.connect.accounts.controller;

import com.andrejKir.connect.accounts.dto.response.AppUserPrivateDetailResponse;
import com.andrejKir.connect.accounts.security.SecurityUser;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.shared.web.ApiPaths;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/users")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/me")
    public AppUserPrivateDetailResponse me(@AuthenticationPrincipal SecurityUser principal) {
        return appUserService.getPrivateDetail(principal.getId());
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deactivate(@AuthenticationPrincipal SecurityUser principal,
                                           HttpServletRequest servletRequest) {
        appUserService.deactivate(principal.getId());

        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}