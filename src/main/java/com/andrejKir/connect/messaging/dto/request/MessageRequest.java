package com.andrejKir.connect.messaging.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MessageRequest(
        @NotBlank @Size(max = 4000) String body
) { }
