package com.example.seabattleonline.web.security;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {
    private final JwtTokenProvider tokenProvider;

    @Override
    public @Nullable Message<?> preSend(
            @NonNull final Message<?> message,
            @NonNull final MessageChannel channel
    ) {
        StompHeaderAccessor accessor = MessageHeaderAccessor
                .getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(
                accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (tokenProvider.validateToken(token)) {
                    var auth = tokenProvider.getAuthentication(token);
                    accessor.setUser(auth);
                } else {
                    throw new IllegalArgumentException("Invalid JWT");
                }
            } else {
                throw new IllegalArgumentException(
                        "Missing Authorization header"
                );
            }
        }
        return message;
    }
}
