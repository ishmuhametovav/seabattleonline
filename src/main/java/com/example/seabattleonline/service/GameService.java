package com.example.seabattleonline.service;

import com.example.seabattleonline.web.dto.game.PlaceShipCommand;
import com.example.seabattleonline.web.dto.game.ShootCommand;

public interface GameService {

    Long createRoom(String player1, String player2);

    void markReady(Long roomId, String player);

    void unmarkReady(Long roomId, String player);

    void placeShip(
            Long roomId,
            String player,
            PlaceShipCommand command
    );

    void clearBoard(Long roomId, String player);

    void makeTurn(
            Long roomId,
            String player,
            ShootCommand command
    );

    void disconnect(Long roomId, String player);

    void getGameInfo(
            Long roomId,
            String player
    );
}
