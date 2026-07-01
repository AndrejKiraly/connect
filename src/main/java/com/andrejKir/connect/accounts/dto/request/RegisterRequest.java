package com.andrejKir.connect.accounts.dto.request;

import com.andrejKir.connect.accounts.validation.MinimumAge;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequest (
    @NotBlank @Email String email,
    @NotBlank @Size(min = 4, max = 40) String username,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank @Size (max = 100) String displayName,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 150)String lastName,
    @MinimumAge(13) @NotNull @Past LocalDate birthDate
) {


}
