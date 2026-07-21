package com.andrejKir.connect.accounts.controller;


import com.andrejKir.connect.accounts.dto.request.ForgotPasswordRequest;
import com.andrejKir.connect.accounts.dto.request.ResetPasswordRequest;
import com.andrejKir.connect.accounts.service.PasswordResetService;
import com.andrejKir.connect.shared.ratelimit.RateLimitPolicy;
import com.andrejKir.connect.shared.ratelimit.RateLimitService;
import com.andrejKir.connect.shared.web.ApiPaths;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.V1 + "/auth/password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final RateLimitService rateLimitService;

    public PasswordResetController(PasswordResetService passwordResetService, RateLimitService rateLimitService) {
        this.passwordResetService = passwordResetService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping(path = "/forgot")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request,
                                               HttpServletRequest servletRequest){
        rateLimitService.check(RateLimitPolicy.PASSWORD_FORGOT_PER_IP, servletRequest.getRemoteAddr());
        rateLimitService.check(RateLimitPolicy.PASSWORD_FORGOT_PER_EMAIL, request.email());
        passwordResetService.generatePasswordResetToken(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request,
                                              HttpServletRequest servletRequest){
        rateLimitService.check(RateLimitPolicy.PASSWORD_RESET_PER_IP, servletRequest.getRemoteAddr());
        passwordResetService.resetPassword(request.resetToken(),request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
