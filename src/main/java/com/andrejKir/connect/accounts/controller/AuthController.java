package com.andrejKir.connect.accounts.controller;

import com.andrejKir.connect.accounts.dto.request.LoginRequest;
import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserResponse;
import com.andrejKir.connect.accounts.security.SecurityUser;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.shared.exception.RateLimitExceededException;
import com.andrejKir.connect.shared.ratelimit.RateLimitService;
import com.andrejKir.connect.shared.web.ApiPaths;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/auth")
public class AuthController {

    private final AppUserService appUserService;
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final RateLimitService rateLimitService;

    public AuthController(AppUserService appUserService, AuthenticationManager authenticationManager,
                          SessionAuthenticationStrategy sessionAuthenticationStrategy,
                          SecurityContextRepository securityContextRepository, RateLimitService rateLimitService) {
        this.appUserService = appUserService;
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/register")
    public ResponseEntity<AppUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        AppUserResponse response = appUserService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AppUserResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                                 HttpServletRequest servletRequest,
                                                 HttpServletResponse servletResponse) {
        ConsumptionProbe probe = rateLimitService.tryLoginByUser(loginRequest.usernameOrEmail());
        if (!probe.isConsumed()){
            long retryAfterSeconds = Math.ceilDiv(probe.getNanosToWaitForRefill(), 1_000_000_000L);
            throw new RateLimitExceededException((retryAfterSeconds));
        }
        var authRequest = UsernamePasswordAuthenticationToken
                .unauthenticated(loginRequest.usernameOrEmail(), loginRequest.password());
        Authentication auth = authenticationManager.authenticate(authRequest);
        sessionAuthenticationStrategy.onAuthentication(auth, servletRequest, servletResponse);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, servletRequest, servletResponse);

        SecurityUser principal = (SecurityUser) auth.getPrincipal();
        return ResponseEntity.ok(appUserService.getById(principal.getId()));
    }
}