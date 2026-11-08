package com.example.seabattleonline.web.dto.game;

import com.example.seabattleonline.domain.game.GameStatus;
import com.example.seabattleonline.domain.game.GridInfo;
import com.example.seabattleonline.domain.room.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;
import java.util.Set;

public record GameInfo(
        @NotNull(message = "grid should not be null")
        GridInfo gridInfo,
        @NotBlank(message = "players set should not be null")
        Set<String> players,
        @NotBlank(message = "current player should not be null")
        String currentPlayer,
        Optional<String> winner,
        @NotNull(message = "game status should not be null")
        GameStatus gameStatus,
        @NotNull(message = "room status should not be null")
        Status roomStatus
) {
}
