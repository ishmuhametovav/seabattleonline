package com.example.seabattleonline.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for login")
public record JwtRequest(
        @NotBlank(message = "username must be not null")
        @Size(max = 255, message = "username must be less than 256")
        @Schema(description = "username", example = "habibu")
        String username,

        @NotBlank(message = "password must be not null")
        @Size(max = 255, message = "password must be less than 256")
        @Schema(description = "password", example = "pass")
        String password
) {
}
