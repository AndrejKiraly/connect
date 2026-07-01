package com.andrejKir.connect.accounts.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest (
    @NotBlank @Size(min = 4, max = 40) String username,
    @NotBlank @Size(min = 8, max = 72) String password
    ){

}
