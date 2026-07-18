package com.andrejKir.connect.accounts.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record ForgotPasswordRequest(
        @Email @NotBlank String email
) {
    public ForgotPasswordRequest {
        email = email != null ? email.trim().toLowerCase(Locale.ROOT) : null;
    }
}
