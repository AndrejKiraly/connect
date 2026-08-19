package com.andrejKir.connect.accounts.controller;

import com.andrejKir.connect.accounts.dto.response.AppUserPrivateDetailResponse;
import com.andrejKir.connect.accounts.dto.response.AppUserPublicDetailResponse;
import com.andrejKir.connect.accounts.dto.response.AppUserPublicSummaryResponse;
import com.andrejKir.connect.accounts.security.SecurityUser;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.shared.web.ApiPaths;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

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

    @GetMapping
    public List<AppUserPublicSummaryResponse> findUsers(@RequestParam(required = false) String q,
                                                        @RequestParam(required = false) String code,
                                                        @AuthenticationPrincipal SecurityUser principal) {
        boolean hasQuery = q != null;
        boolean hasCode = code != null;
        if (hasQuery == hasCode) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide exactly one of 'q' or 'code'");
        }
        return hasQuery
                ? appUserService.searchUsersByDisplayName(q, principal.getId())
                : appUserService.findUserByInviteCode(code, principal.getId());
    }

    @GetMapping("/{id}")
    public AppUserPublicDetailResponse getUser(@PathVariable UUID id) {
        return appUserService.getPublicDetail(id);
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