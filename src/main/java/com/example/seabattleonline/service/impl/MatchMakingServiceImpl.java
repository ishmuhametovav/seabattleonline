package com.example.seabattleonline.service.impl;

import com.example.seabattleonline.domain.exception.AccessDeniedException;
import com.example.seabattleonline.domain.exception.PlayerAlreadyInQueueException;
import com.example.seabattleonline.service.GameService;
import com.example.seabattleonline.service.MatchMakingService;
import com.example.seabattleonline.web.dto.matchmaking.MatchMakingEvent;
import com.example.seabattleonline.web.dto.matchmaking.MatchMakingEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
public class MatchMakingServiceImpl implements MatchMakingService {

    private final GameService gameService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final Queue<String> playersQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void findMatch(final String player) {
        if (player == null || playersQueue.contains(player)) {
            throw new PlayerAlreadyInQueueException(
                    "Player " + player + " is already in queue"
            );
        }

        if (!playersQueue.isEmpty()) {
            var anotherPlayer = playersQueue.poll();
            simpMessagingTemplate.convertAndSendToUser(
                    player,
                    "/queue/matchmaking/event",
                    new MatchMakingEvent(MatchMakingEventType.CONNECTION)
            );

            simpMessagingTemplate.convertAndSendToUser(
                    anotherPlayer,
                    "/queue/matchmaking/event",
                    new MatchMakingEvent(MatchMakingEventType.CONNECTION)
            );

            gameService.createRoom(player, anotherPlayer);

        } else {
            playersQueue.add(player);
            simpMessagingTemplate.convertAndSendToUser(
                    player,
                    "/queue/matchmaking/event",
                    new MatchMakingEvent(MatchMakingEventType.IN_QUEUE)
            );
        }
    }

    @Override
    //TODO if player disconnects while game
    public void disconnect(final String player) {
        if (!playersQueue.remove(player)) {
            throw new AccessDeniedException();
        }

        simpMessagingTemplate.convertAndSendToUser(
                player,
                "/queue/matchmaking/event",
                new MatchMakingEvent(MatchMakingEventType.DISCONNECTION)
        );
    }
}
