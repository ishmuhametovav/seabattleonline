package com.example.seabattleonline.domain.game;

import domain.game.board.Board;
import domain.game.exception.*;
import domain.game.ship.FleetSpec;
import domain.game.ship.ShotResult;
import domain.game.vector.Vector;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public class Game {
    private final Board board1;
    private final Board board2;

    private final Set<PlayerSlot> readyPlayers;
    private GameStatus status;
    private PlayerSlot currentPlayer;
    private PlayerSlot winner;

    public Game(int size) {
        board1 = new Board(size, FleetSpec.classic());
        board2 = new Board(size, FleetSpec.classic());
        readyPlayers = EnumSet.noneOf(PlayerSlot.class);
        status = GameStatus.SHIP_PLACEMENT;
        currentPlayer = PlayerSlot.PLAYER1;
        winner = null;
    }

    public void clearBoard(PlayerSlot player) {
        if (status != GameStatus.SHIP_PLACEMENT) {
            throw new GameAlreadyStartedException();
        }
        Board currentPlayerBoard = player == PlayerSlot.PLAYER1
                ? board1 : board2;
        currentPlayerBoard.clear();
        readyPlayers.remove(player);
    }

    public void markReady(PlayerSlot player) {
        if (status != GameStatus.SHIP_PLACEMENT) {
            throw new GameAlreadyStartedException();
        }

        Board currentPlayerBoard = player == PlayerSlot.PLAYER1
                ? board1 : board2;

        if (!currentPlayerBoard.allShipsPlaced()) {
            throw new NotAllShipsPlacedException();
        }
        readyPlayers.add(player);
        if (readyPlayers.size() == 2) {
            status = GameStatus.IN_PROGRESS;
            currentPlayer = PlayerSlot.PLAYER1;
        }
    }

    public void unmarkReady(PlayerSlot player) {
        if (status != GameStatus.SHIP_PLACEMENT) {
            throw new GameAlreadyStartedException();
        }
        readyPlayers.remove(player);
    }

    public void placeShip(PlayerSlot player, Vector start, int length, boolean horizontal) {
        if (status != GameStatus.SHIP_PLACEMENT) {
            throw new GameAlreadyStartedException();
        }
        if (readyPlayers.contains(player)) {
            throw new PlayerAlreadyReadyException();
        }
        if (player.equals(PlayerSlot.PLAYER1)) board1.placeShip(start, length, horizontal);
        else board2.placeShip(start, length, horizontal);
    }

    public ShotResult makeTurn(Vector coords, PlayerSlot player) {
        if (status == GameStatus.FINISHED) {
            throw new GameAlreadyOverException();
        }
        if (currentPlayer != player) {
            throw new AnotherPlayerTurnException();
        }

        if (status != GameStatus.IN_PROGRESS) {
            throw new GameNotInProgressException();
        }

        PlayerSlot secondPlayer = currentPlayer == PlayerSlot.PLAYER1
                ? PlayerSlot.PLAYER2 : PlayerSlot.PLAYER1;
        Board secondPlayerBoard = currentPlayer == PlayerSlot.PLAYER1
                ? board2 : board1;

        return processTurn(currentPlayer, secondPlayer, secondPlayerBoard, coords);
    }

    public void reset() {
        board1.clear();
        board2.clear();
        readyPlayers.clear();
        status = GameStatus.SHIP_PLACEMENT;
        currentPlayer = PlayerSlot.PLAYER1;
        winner = null;
    }

    public Optional<PlayerSlot> getWinner() {
        return Optional.ofNullable(winner);
    }

    public GridInfo getGridInfo(PlayerSlot player) {
        Board playerBoard = player == PlayerSlot.PLAYER1
                ? board1 : board2;
        Board enemyBoard = player == PlayerSlot.PLAYER1
                ? board2 : board1;
        return new GridInfo(playerBoard.getGrid(), enemyBoard.getFilteredGrid());
    }

    private ShotResult processTurn(
            PlayerSlot player1,
            PlayerSlot player2,
            Board board,
            Vector coords
    ) {
        ShotResult result = board.shot(coords);

        if (result == ShotResult.MISS) {
            currentPlayer = player2;
        } else if (board.allShipsDestroyed()) {
            status = GameStatus.FINISHED;
            winner = player1;
        }

        return result;
    }
}
