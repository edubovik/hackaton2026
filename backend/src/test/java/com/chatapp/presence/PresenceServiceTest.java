package com.chatapp.presence;

import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.presence.entity.PresenceState;
import com.chatapp.presence.entity.UserPresence;
import com.chatapp.presence.repository.UserPresenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.chatapp.common.BrokerTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock UserPresenceRepository presenceRepository;
    @Mock UserRepository userRepository;
    @Mock BrokerTemplate messagingTemplate;

    PresenceService service;

    User userA;

    @BeforeEach
    void setUp() {
        service = new PresenceService(presenceRepository, userRepository, messagingTemplate);

        userA = makeUser(1L, "alice");

        when(presenceRepository.findById(any())).thenReturn(Optional.empty());
        when(presenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void singleConnect_setsOnline() {
        service.onConnect(userA);

        ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
        verify(presenceRepository).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(PresenceState.ONLINE);
    }

    @Test
    void twoConnects_bothOnline_singleDisconnect_remainsOnline() {
        service.onConnect(userA);
        service.onConnect(userA);

        reset(presenceRepository, messagingTemplate);

        service.onDisconnect(userA);

        // still has one connection — should NOT go offline
        verify(messagingTemplate, never()).send(eq("/topic/presence"), any(Object.class));
    }

    @Test
    void lastDisconnect_setsOffline() {
        service.onConnect(userA);

        reset(presenceRepository, messagingTemplate);
        when(presenceRepository.findById(any())).thenReturn(Optional.empty());
        when(presenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.onDisconnect(userA);

        ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
        verify(presenceRepository).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(PresenceState.OFFLINE);
    }

    @Test
    void heartbeat_alwaysSetsOnline() {
        service.onHeartbeat(userA);

        ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
        verify(presenceRepository).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(PresenceState.ONLINE);
    }

    @Test
    void heartbeat_onAfkUser_restoresOnline() {
        UserPresence afkPresence = new UserPresence(userA.getId());
        afkPresence.setState(PresenceState.AFK);
        when(presenceRepository.findById(userA.getId())).thenReturn(Optional.of(afkPresence));

        service.onHeartbeat(userA);

        ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
        verify(presenceRepository).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(PresenceState.ONLINE);
    }

    @Test
    void getConnectionCount_returnsCorrectCount() {
        assertThat(service.getConnectionCount(userA.getId())).isZero();
        service.onConnect(userA);
        assertThat(service.getConnectionCount(userA.getId())).isEqualTo(1);
        service.onConnect(userA);
        assertThat(service.getConnectionCount(userA.getId())).isEqualTo(2);
        service.onDisconnect(userA);
        assertThat(service.getConnectionCount(userA.getId())).isEqualTo(1);
    }

    // ---- helpers ----

    private static User makeUser(Long id, String username) {
        try {
            User u = new User();
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(u, id);
            u.setUsername(username);
            u.setEmail(username + "@test.com");
            u.setPassword("pw");
            return u;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
