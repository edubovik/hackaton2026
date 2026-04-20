package com.chatapp.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class CookieHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String cookieHeader = request.getHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            for (String part : cookieHeader.split(";")) {
                String trimmed = part.trim();
                if (trimmed.startsWith("access_token=")) {
                    attributes.put("access_token", trimmed.substring("access_token=".length()));
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}
