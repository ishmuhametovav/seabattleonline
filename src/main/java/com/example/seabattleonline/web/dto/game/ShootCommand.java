package com.example.seabattleonline.web.dto.game;

import jakarta.validation.constraints.NotNull;

public record ShootCommand(
        @NotNull(message = "position must be not null")
        VectorDto position
) {
}
