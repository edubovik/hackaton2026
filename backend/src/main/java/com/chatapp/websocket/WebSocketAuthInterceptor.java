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

import java.util.List;

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
            var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            accessor.setUser(auth);
        });

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> nativeHeaders = accessor.getNativeHeader("cookie");
        if (nativeHeaders == null || nativeHeaders.isEmpty()) return null;
        String cookieHeader = nativeHeaders.get(0);
        for (String part : cookieHeader.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("access_token=")) {
                return trimmed.substring("access_token=".length());
            }
        }
        return null;
    }
}
