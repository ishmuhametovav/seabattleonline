package com.example.seabattleonline.web.security.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {
    @EventListener
    public void onDisconnect(final SessionDisconnectEvent event) {

    }
}
