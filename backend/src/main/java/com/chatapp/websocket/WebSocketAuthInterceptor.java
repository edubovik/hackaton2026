package com.chatapp.websocket;

import com.chatapp.auth.JwtService;
import com.chatapp.auth.repository.UserRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public WebSocketAuthInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String token = extractToken(accessor);
        if (token == null || !jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or missing access_token cookie");
        }

        Long userId = jwtService.extractUserId(token);
        userRepository.findById(userId).ifPresent(user -> {
            // Principal name must be String.valueOf(userId) so that convertAndSendToUser
            // in MessageService (which uses the numeric ID) can resolve the session.
            var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()) {
                @Override public String getName() { return String.valueOf(user.getId()); }
            };
            accessor.setUser(auth);
        });

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Token is captured from the HTTP upgrade Cookie header by CookieHandshakeInterceptor
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null) {
            Object token = attrs.get("access_token");
            if (token instanceof String s) return s;
        }
        return null;
    }
}
