package com.example.seabattleonline.web.controller;

import com.example.seabattleonline.service.GameService;
import com.example.seabattleonline.web.dto.game.PlaceShipCommand;
import com.example.seabattleonline.web.dto.game.ShootCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class GameController {

    private static final String CAN_ACCESS_ROOM =
            "@customSecurityExpression.canAccessRoom("
                    + "#roomId, authentication.name)";

    private final GameService gameService;

    @MessageMapping("/room/{id}/mark-ready")
    @PreAuthorize(CAN_ACCESS_ROOM)
    public void markReady(
            @DestinationVariable final Long roomId,
            final Principal principal
    ) {
        gameService.markReady(roomId, principal.getName());
    }

    @MessageMapping("/room/{id}/unmark-ready")
    @PreAuthorize(CAN_ACCESS_ROOM)
    public void unmarkReady(
            @DestinationVariable final Long roomId,
            final Principal principal
    ) {
        gameService.unmarkReady(roomId, principal.getName());
    }

    @MessageMapping("/room/{id}/place-ship")
    @PreAuthorize(CAN_ACCESS_ROOM)
    public void placeShip(
            @DestinationVariable final Long roomId,
            @Payload final PlaceShipCommand command,
            final Principal principal
    ) {
        gameService.placeShip(roomId, principal.getName(), command);
    }

    @MessageMapping("/room/{id}/clear-board")
    @PreAuthorize(CAN_ACCESS_ROOM)
    public void clearBoard(
            @DestinationVariable final Long roomId,
            final Principal principal
    ) {
        gameService.clearBoard(roomId, principal.getName());
    }

    @MessageMapping("/room/{id}/make-turn")
    @PreAuthorize(CAN_ACCESS_ROOM)
    public void makeTurn(
            @DestinationVariable final Long roomId,
            @Payload final ShootCommand command,
            final Principal principal
    ) {
        gameService.makeTurn(roomId, principal.getName(), command);
    }

    @MessageMapping("/room/{id}/get-game-info")
    @PreAuthorize(CAN_ACCESS_ROOM)
    public void getGameInfo(
            @DestinationVariable final Long roomId,
            final Principal principal
    ) {
        gameService.getGameInfo(roomId, principal.getName());
    }
}
