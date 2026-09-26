package com.example.seabattleonline.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login response")
public record JwtResponse(
        @Schema(description = "id", example = "1")
        Long id,

        @Schema(description = "username", example = "habibu")
        String username,

        @Schema(description = "access token")
        String accessToken,

        @Schema(description = "refresh token")
        String refreshToken
) {
}
