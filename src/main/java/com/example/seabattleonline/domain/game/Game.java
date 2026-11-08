package com.example.seabattleonline.domain.game;

import com.example.seabattleonline.domain.game.board.Board;
import com.example.seabattleonline.domain.game.exception.*;
import com.example.seabattleonline.domain.game.ship.FleetSpec;
import com.example.seabattleonline.domain.game.ship.ShotResult;
import com.example.seabattleonline.domain.game.vector.Vector;

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

    public Game(final int size) {
        board1 = new Board(size, FleetSpec.classic());
        board2 = new Board(size, FleetSpec.classic());
        readyPlayers = EnumSet.noneOf(PlayerSlot.class);
        status = GameStatus.SHIP_PLACEMENT;
        currentPlayer = PlayerSlot.PLAYER1;
        winner = null;
    }

    public void clearBoard(final PlayerSlot player) {
        if (status != GameStatus.SHIP_PLACEMENT) {
            throw new GameAlreadyStartedException();
        }
        Board currentPlayerBoard = player == PlayerSlot.PLAYER1
                ? board1 : board2;
        currentPlayerBoard.clear();
        readyPlayers.remove(player);
    }

    public void markReady(final PlayerSlot player) {
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

    public void unmarkReady(final PlayerSlot player) {
        if (status != GameStatus.SHIP_PLACEMENT) {
            throw new GameAlreadyStartedException();
        }
        readyPlayers.remove(player);
    }

    public void placeShip(
            final PlayerSlot player,
            final Vector start,
            final int length,
            final boolean horizontal) {
        if (status != GameStatus.SHIP_PLACEMENT) {
            throw new GameAlreadyStartedException();
        }
        if (readyPlayers.contains(player)) {
            throw new PlayerAlreadyReadyException();
        }
        if (player.equals(PlayerSlot.PLAYER1)) {
            board1.placeShip(start, length, horizontal);
        } else {
            board2.placeShip(start, length, horizontal);
        }
    }

    public ShotResult makeTurn(
            final Vector coords,
            final PlayerSlot player
    ) {
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

        return processTurn(
                currentPlayer,
                secondPlayer,
                secondPlayerBoard,
                coords
        );
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

    public PlayerSlot getCurrentPlayer() {
        return currentPlayer;
    }

    public GridInfo getGridInfo(final PlayerSlot player) {
        Board playerBoard = player == PlayerSlot.PLAYER1
                ? board1 : board2;
        Board enemyBoard = player == PlayerSlot.PLAYER1
                ? board2 : board1;
        return new GridInfo(
                playerBoard.getGrid(),
                enemyBoard.getFilteredGrid()
        );
    }

    public GameStatus getStatus() {
        return status;
    }

    private ShotResult processTurn(
            final PlayerSlot player1,
            final PlayerSlot player2,
            final Board board,
            final Vector coords
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
