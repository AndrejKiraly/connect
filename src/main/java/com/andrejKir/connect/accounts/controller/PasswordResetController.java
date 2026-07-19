package com.andrejKir.connect.accounts.controller;


import com.andrejKir.connect.accounts.dto.request.ForgotPasswordRequest;
import com.andrejKir.connect.accounts.dto.request.ResetPasswordRequest;
import com.andrejKir.connect.accounts.service.PasswordResetService;
import com.andrejKir.connect.shared.web.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.V1 + "/auth/password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping(path = "/forgot")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request){
        passwordResetService.generatePasswordResetToken(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request){
        passwordResetService.resetPassword(request.resetToken(),request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
