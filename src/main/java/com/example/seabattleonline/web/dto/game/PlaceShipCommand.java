package com.example.seabattleonline.web.dto.game;

import jakarta.validation.constraints.NotNull;

public record PlaceShipCommand(
        @NotNull(message = "position should not be null")
        VectorDto position,

        @NotNull(message = "ship orientation should not be null")
        Boolean horizontal,

        @NotNull(message = "ship length should not be null")
        Integer length
) {
}
