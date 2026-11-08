package com.example.seabattleonline.service.impl;

import com.example.seabattleonline.domain.room.Room;
import com.example.seabattleonline.repository.RoomRepository;
import com.example.seabattleonline.service.GameService;
import com.example.seabattleonline.web.dto.game.GameInfo;
import com.example.seabattleonline.web.dto.game.PlaceShipCommand;
import com.example.seabattleonline.web.dto.game.ShootCommand;
import com.example.seabattleonline.web.dto.game.ShotResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RoomRepository roomRepository;

    @Override
    public Long createRoom(final String player1, final String player2) {

        Room room = new Room(null, player1, player2);
        var updatedRoom = roomRepository.save(room);
        simpMessagingTemplate.convertAndSendToUser(
                player1,
                "/queue/matchmaking/roomid",
                updatedRoom.getId()
        );

        simpMessagingTemplate.convertAndSendToUser(
                player2,
                "/queue/matchmaking/roomid",
                updatedRoom.getId()
        );
        return updatedRoom.getId();
    }

    @Override
    public void markReady(final Long roomId, final String player) {
        getRoomOrThrow(roomId).markReady(player);
        getGameInfo(roomId, player);
    }

    @Override
    public void unmarkReady(final Long roomId, final String player) {
        getRoomOrThrow(roomId).unmarkReady(player);
        getGameInfo(roomId, player);
    }

    @Override
    public void placeShip(
            final Long roomId,
            final String player,
            final PlaceShipCommand command
    ) {
        getRoomOrThrow(roomId).placeShip(player, command);
        getGameInfo(roomId, player);
    }

    @Override
    public void clearBoard(final Long roomId, final String player) {
        getRoomOrThrow(roomId).clearBoard(player);
        getGameInfo(roomId, player);
    }

    @Override
    public void makeTurn(
            final Long roomId,
            final String player,
            final ShootCommand command
    ) {
        var room = getRoomOrThrow(roomId);
        var shotResult = room.makeTurn(player, command);

        var winner = room.getWinner();

        ShotResultDto result = new ShotResultDto(
                command.position(),
                shotResult,
                winner
        );
        var players = room.getPlayers().toArray();

        simpMessagingTemplate.convertAndSendToUser(
                players[0].toString(),
                "/room/" + room.getId() + "/shot-result",
                result
        );

        simpMessagingTemplate.convertAndSendToUser(
                players[1].toString(),
                "/room/" + room.getId() + "/shot-result",
                result
        );

        if (winner.isPresent()) {
            roomRepository.deleteById(roomId);
        }
    }

    @Override
    public void disconnect(
            final Long roomId,
            final String player
    ) {
        var room = getRoomOrThrow(roomId);
        var players = room.getPlayers().toArray();

        simpMessagingTemplate.convertAndSendToUser(
                players[0].toString(),
                "/room/" + room.getId() + "/disconnect",
                "player " + player + " disconnected"
        );

        simpMessagingTemplate.convertAndSendToUser(
                players[1].toString(),
                "/room/" + room.getId() + "/disconnect",
                "player " + player + " disconnected"
        );

        roomRepository.deleteById(roomId);
    }

    @Override
    public void getGameInfo(
            final Long roomId,
            final String player
    ) {
        var room = getRoomOrThrow(roomId);

        GameInfo playerGameInfo = new GameInfo(
                room.getGridInfo(player),
                room.getPlayers(),
                room.getCurrentPlayer(),
                room.getWinner(),
                room.getGameStatus(),
                room.getStatus()
        );

        simpMessagingTemplate.convertAndSendToUser(
                player,
                "/room/" + room.getId() + "/game-info",
                playerGameInfo
        );
    }

    private Room getRoomOrThrow(final Long roomId) {
        var room = roomRepository.findById(roomId);
        if (room.isEmpty()) {
            throw new IllegalArgumentException(
                    "Room id " + roomId + " does not exist"
            );
        }
        return room.get();
    }
}
