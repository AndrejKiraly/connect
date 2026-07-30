package com.andrejKir.connect.messaging.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(
        @NotBlank @Size(max = 4000) String body
) { }
