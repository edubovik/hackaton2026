package com.chatapp.presence;

import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.presence.entity.PresenceState;
import com.chatapp.presence.entity.UserPresence;
import com.chatapp.presence.repository.UserPresenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketMessagingAutoConfiguration",
                "app.jwt.secret=integration-test-secret-that-is-long-enough-for-hs256",
                "app.stomp.relay-host=localhost",
                "app.stomp.relay-port=61613",
                "app.stomp.login=guest",
                "app.stomp.passcode=guest"
        }
)
class PresenceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean SimpMessagingTemplate messagingTemplate;

    @Autowired PresenceService presenceService;
    @Autowired UserPresenceRepository presenceRepository;
    @Autowired UserRepository userRepository;

    User testUser;

    @BeforeEach
    void setUp() {
        presenceRepository.deleteAll();
        userRepository.deleteAll();

        User u = new User();
        u.setEmail("presence@test.com");
        u.setUsername("presenceuser");
        u.setPassword("hashed");
        testUser = userRepository.save(u);
    }

    @Test
    void onConnect_persistsOnlineState() {
        presenceService.onConnect(testUser);

        UserPresence saved = presenceRepository.findById(testUser.getId()).orElseThrow();
        assertThat(saved.getState()).isEqualTo(PresenceState.ONLINE);
    }

    @Test
    void onDisconnect_afterLastConnection_persistsOfflineState() {
        presenceService.onConnect(testUser);
        presenceService.onDisconnect(testUser);

        UserPresence saved = presenceRepository.findById(testUser.getId()).orElseThrow();
        assertThat(saved.getState()).isEqualTo(PresenceState.OFFLINE);
    }

    @Test
    void onDisconnect_withMultipleConnections_remainsOnline() {
        presenceService.onConnect(testUser);
        presenceService.onConnect(testUser);
        presenceService.onDisconnect(testUser);

        UserPresence saved = presenceRepository.findById(testUser.getId()).orElseThrow();
        assertThat(saved.getState()).isEqualTo(PresenceState.ONLINE);
    }

    @Test
    void onHeartbeat_persistsOnlineState() {
        presenceService.onHeartbeat(testUser);

        UserPresence saved = presenceRepository.findById(testUser.getId()).orElseThrow();
        assertThat(saved.getState()).isEqualTo(PresenceState.ONLINE);
    }

    @Test
    void sweepStaleConnections_marksStaleUsersAsAfk() {
        // persist an ONLINE presence record
        UserPresence stale = new UserPresence(testUser.getId());
        stale.setState(PresenceState.ONLINE);
        presenceRepository.save(stale);
        presenceRepository.flush();

        // artificially age the record via reflection so it looks stale
        presenceRepository.findById(testUser.getId()).ifPresent(p -> {
            try {
                var field = UserPresence.class.getDeclaredField("updatedAt");
                field.setAccessible(true);
                field.set(p, Instant.now().minusSeconds(120));
                presenceRepository.save(p);
                presenceRepository.flush();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        presenceService.sweepStaleConnections();

        UserPresence swept = presenceRepository.findById(testUser.getId()).orElseThrow();
        assertThat(swept.getState()).isEqualTo(PresenceState.AFK);
    }
}
