package com.chatapp.websocket;

import com.chatapp.auth.entity.User;
import com.chatapp.presence.PresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class WebSocketEventListener {

    private final PresenceService presenceService;

    public WebSocketEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof User user) {
            presenceService.onConnect(user);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof User user) {
            presenceService.onDisconnect(user);
        }
    }
}
