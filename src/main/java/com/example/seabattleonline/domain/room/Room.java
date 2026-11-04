package com.example.seabattleonline.domain.room;

import com.example.seabattleonline.domain.exception.RoomAlreadyInActiveException;
import com.example.seabattleonline.domain.game.Game;
import com.example.seabattleonline.domain.game.GameStatus;
import com.example.seabattleonline.domain.game.GridInfo;
import com.example.seabattleonline.domain.game.PlayerSlot;
import com.example.seabattleonline.domain.game.ship.ShotResult;
import com.example.seabattleonline.domain.game.vector.Vector;
import com.example.seabattleonline.web.dto.game.PlaceShipCommand;
import com.example.seabattleonline.web.dto.game.ShootCommand;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Room {
    private final Map<String, PlayerSlot> playerSlots = new HashMap<>();
    private final Game game;
    @Getter
    @Setter
    private Long id;
    @Getter
    private Status status;

    public Room(final Long id, final String player1, final String player2) {
        this.id = id;
        if (player1.equals(player2)) {
            throw new IllegalArgumentException(
                    "Player 1 and Player 2 cannot be the same"
            );
        }

        playerSlots.put(player1, PlayerSlot.PLAYER1);
        playerSlots.put(player2, PlayerSlot.PLAYER2);
        //TODO size
        game = new Game(10);
        status = Status.ACTIVE;
    }

    public void markReady(final String player) {
        if (status == Status.INACTIVE) {
            throw new RoomAlreadyInActiveException();
        }
        if (!playerSlots.containsKey(player)) {
            throw new IllegalArgumentException(
                    "Player " + player + " does not exist"
            );
        }
        game.markReady(playerSlots.get(player));
    }

    public void unmarkReady(final String player) {
        if (status == Status.INACTIVE) {
            throw new RoomAlreadyInActiveException();
        }
        if (!playerSlots.containsKey(player)) {
            throw new IllegalArgumentException(
                    "Player " + player + " does not exist"
            );
        }
        game.unmarkReady(playerSlots.get(player));
    }

    public void placeShip(
            final String player,
            final PlaceShipCommand command
    ) {
        if (status == Status.INACTIVE) {
            throw new RoomAlreadyInActiveException();
        }
        if (!playerSlots.containsKey(player)) {
            throw new IllegalArgumentException(
                    "Player " + player + " does not exist"
            );
        }
        Vector coords = new Vector(
                command.position().x(),
                command.position().y()
        );
        game.placeShip(
                playerSlots.get(player),
                coords,
                command.length(),
                command.horizontal()
        );
    }

    public void clearBoard(final String player) {
        if (status == Status.INACTIVE) {
            throw new RoomAlreadyInActiveException();
        }
        if (!playerSlots.containsKey(player)) {
            throw new IllegalArgumentException(
                    "Player " + player + " does not exist"
            );
        }
        game.clearBoard(playerSlots.get(player));
    }

    public ShotResult makeTurn(
            final String player,
            final ShootCommand command
    ) {
        if (status == Status.INACTIVE) {
            throw new RoomAlreadyInActiveException();
        }
        if (!playerSlots.containsKey(player)) {
            throw new IllegalArgumentException(
                    "Player " + player + " does not exist"
            );
        }
        Vector coords = new Vector(
                command.position().x(),
                command.position().y()
        );
        var result = game.makeTurn(coords, playerSlots.get(player));

        if (!game.getWinner().isEmpty()) {
            status = Status.INACTIVE;
        }

        return result;
    }

    public Optional<String> getWinner() {
        var result = game.getWinner();
        if (result.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> winner = Optional.empty();
        for (var pair : playerSlots.entrySet()) {
            if (pair.getValue().equals(result.get())) {
                winner = Optional.of(pair.getKey());
            }
        }
        return winner;
    }

    public Set<String> getPlayers() {
        return playerSlots.keySet();
    }

    public GridInfo getGridInfo(final String player) {
        if (!playerSlots.containsKey(player)) {
            throw new IllegalArgumentException(
                    "Player " + player + " does not exist"
            );
        }
        return game.getGridInfo(playerSlots.get(player));
    }

    public GameStatus getGameStatus() {
        return game.getStatus();
    }
}
