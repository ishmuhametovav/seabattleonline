package com.example.seabattleonline.web.dto.game;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

//TODO max size depends on board size of the room
@Schema(description = "Vector dto")
public record VectorDto(
        @NotNull(message = "x coordinate should not be null")
        @Min(value = 0, message = "x coordinate should be within borders")
        @Max(value = 9, message = "x coordinate should be within borders")
        Integer x,

        @NotNull(message = "y coordinate should not be null")
        @Min(value = 0, message = "y coordinate should be within borders")
        @Max(value = 9, message = "y coordinate should be within borders")
        Integer y
) {
}
