package com.andrejKir.connect.accounts.dto.request;

import com.andrejKir.connect.accounts.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @StrongPassword String newPassword,
        @NotBlank @Size(max = 100) String resetToken
) {
}
