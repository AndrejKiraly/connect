package com.andrejKir.connect.accounts.dto.request;


import com.andrejKir.connect.accounts.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record LoginRequest (
    @NotBlank @Size(min = 4, max = 256) String usernameOrEmail,
    @NotBlank @Size(max = StrongPassword.MAX_LENGTH) String password
    ){

    public LoginRequest {
        usernameOrEmail = usernameOrEmail != null ? usernameOrEmail.trim().toLowerCase(Locale.ROOT) : null;
    }
}
