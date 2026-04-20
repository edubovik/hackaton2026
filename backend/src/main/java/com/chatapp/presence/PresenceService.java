package com.chatapp.presence;

import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.presence.dto.PresenceUpdate;
import com.chatapp.presence.entity.PresenceState;
import com.chatapp.presence.entity.UserPresence;
import com.chatapp.presence.repository.UserPresenceRepository;
import com.chatapp.common.BrokerTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PresenceService {

    private final UserPresenceRepository presenceRepository;
    private final UserRepository userRepository;
    private final BrokerTemplate messagingTemplate;

    private final ConcurrentHashMap<Long, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();

    public PresenceService(UserPresenceRepository presenceRepository,
                           UserRepository userRepository,
                           BrokerTemplate messagingTemplate) {
        this.presenceRepository = presenceRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void onConnect(User user) {
        connectionCounts.computeIfAbsent(user.getId(), id -> new AtomicInteger(0)).incrementAndGet();
        setState(user.getId(), user.getUsername(), PresenceState.ONLINE);
    }

    @Transactional
    public void onDisconnect(User user) {
        AtomicInteger count = connectionCounts.get(user.getId());
        if (count == null || count.decrementAndGet() <= 0) {
            connectionCounts.remove(user.getId());
            setState(user.getId(), user.getUsername(), PresenceState.OFFLINE);
        }
    }

    @Transactional
    public void onHeartbeat(User user) {
        setState(user.getId(), user.getUsername(), PresenceState.ONLINE);
    }

    @Transactional
    public void sweepStaleConnections() {
        Instant cutoff = Instant.now().minusSeconds(60);
        List<UserPresence> stale = presenceRepository.findStaleActive(PresenceState.OFFLINE, cutoff);
        for (UserPresence presence : stale) {
            presence.setState(PresenceState.AFK);
            presenceRepository.save(presence);
            userRepository.findById(presence.getUserId()).ifPresent(user ->
                    broadcast(user.getId(), user.getUsername(), PresenceState.AFK));
        }
    }

    public int getConnectionCount(Long userId) {
        AtomicInteger count = connectionCounts.get(userId);
        return count == null ? 0 : count.get();
    }

    private void setState(Long userId, String username, PresenceState state) {
        UserPresence presence = presenceRepository.findById(userId)
                .orElseGet(() -> new UserPresence(userId));
        presence.setState(state);
        presenceRepository.save(presence);
        broadcast(userId, username, state);
    }

    private void broadcast(Long userId, String username, PresenceState state) {
        messagingTemplate.send("/topic/presence",
                new PresenceUpdate(userId, username, state.name()));
    }
}
