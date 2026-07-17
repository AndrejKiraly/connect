package com.andrejKir.connect.accounts.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record LoginRequest (
    @NotBlank @Size(min = 4, max = 256) String usernameOrEmail,
    @NotBlank @Size(max = 72) String password
    ){

    public LoginRequest {
        usernameOrEmail = usernameOrEmail != null ? usernameOrEmail.trim().toLowerCase(Locale.ROOT) : null;
    }
}
