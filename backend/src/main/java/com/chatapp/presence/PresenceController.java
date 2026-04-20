package com.chatapp.presence;

import com.chatapp.auth.entity.User;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @MessageMapping("/presence/heartbeat")
    public void heartbeat(@AuthenticationPrincipal User user) {
        presenceService.onHeartbeat(user);
    }
}
