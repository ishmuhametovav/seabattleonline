package com.example.seabattleonline.web.controller;

import com.example.seabattleonline.service.MatchMakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MatchMakingController {

    private final MatchMakingService matchMakingService;

    @MessageMapping("/matchmaking/find-match")
    public void findMatch(final Principal principal) {
        matchMakingService.findMatch(principal.getName());
    }

    @MessageMapping("/matchmaking/disconnect")
    public void disconnect(final Principal principal) {
        matchMakingService.disconnect(principal.getName());
    }
}
