package com.andrejKir.connect.accounts.dto.request;

import com.andrejKir.connect.accounts.validation.MinimumAge;
import com.andrejKir.connect.accounts.validation.StrongPassword;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Locale;

public record RegisterRequest (
    @NotBlank @Email String email,
    @Pattern( regexp = "^[a-zA-Z0-9._-]+$")@NotBlank @Size(min = 4, max = 40) String username,
    @NotBlank @StrongPassword String password,
    @NotBlank @Size (max = 100) String displayName,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 150)String lastName,
    @MinimumAge(13) @NotNull @Past LocalDate birthDate
) {
    public RegisterRequest {
        email = email != null ? email.trim().toLowerCase(Locale.ROOT) : null;
        username = username != null ? username.trim().toLowerCase(Locale.ROOT) : null;
    }

}
