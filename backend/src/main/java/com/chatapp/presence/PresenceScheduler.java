package com.chatapp.presence;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PresenceScheduler {

    private final PresenceService presenceService;

    public PresenceScheduler(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @Scheduled(fixedDelay = 10_000)
    public void sweepStaleConnections() {
        presenceService.sweepStaleConnections();
    }
}
