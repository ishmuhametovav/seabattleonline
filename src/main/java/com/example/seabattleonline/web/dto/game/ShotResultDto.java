package com.example.seabattleonline.web.dto.game;

import com.example.seabattleonline.domain.game.ship.ShotResult;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

public record ShotResultDto(
        @NotNull(message = "coordinates should be not null")
        VectorDto coords,
        @NotNull(message = "shot result should be not null")
        ShotResult result,
        @NotNull(message = "winner should not be null")
        Optional<String> winner
) {
}
