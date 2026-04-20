package com.chatapp.message;

import com.chatapp.auth.dto.LoginRequest;
import com.chatapp.auth.dto.RegisterRequest;
import com.chatapp.message.dto.MessagePage;
import com.chatapp.message.entity.Message;
import com.chatapp.message.repository.MessageRepository;
import com.chatapp.room.dto.CreateRoomRequest;
import com.chatapp.room.dto.RoomSummaryDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.jwt.secret=integration-test-secret-that-is-long-enough-for-hs256",
                "app.stomp.relay-port=61613"
        }
)
class MessageFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static RabbitMQContainer rabbit = createRabbit();

    @SuppressWarnings("deprecation")
    private static RabbitMQContainer createRabbit() {
        RabbitMQContainer c = new RabbitMQContainer("rabbitmq:3-management");
        c.withPluginsEnabled("rabbitmq_stomp");
        c.addExposedPort(61613);
        return c;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("app.stomp.relay-host", rabbit::getHost);
        registry.add("app.stomp.relay-port", () -> rabbit.getMappedPort(61613));
        registry.add("app.stomp.login", () -> "guest");
        registry.add("app.stomp.passcode", () -> "guest");
    }

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired MessageRepository messageRepository;
    @Autowired MessagePublisher messagePublisher;

    private static final AtomicInteger seq = new AtomicInteger(0);

    @Test
    void publishRoomMessage_persistedToDatabase() {
        int n = seq.incrementAndGet();
        String ownerCookie = registerAndLogin("flow" + n + "@t.com", "flow" + n);
        Long roomId = createRoom("FlowRoom" + n, ownerCookie);
        Long senderId = getMyId(ownerCookie);

        messagePublisher.publishRoomMessage(new com.chatapp.message.dto.ChatMessageEvent(
                "ROOM", roomId, senderId, "flow" + n, null, "Hello from test " + n, null));

        await().atMost(5, SECONDS).untilAsserted(() -> {
            List<Message> saved = messageRepository.findRoomHistory(roomId, null,
                    org.springframework.data.domain.PageRequest.of(0, 10));
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getContent()).isEqualTo("Hello from test " + n);
        });
    }

    @Test
    void publishRoomMessage_appearsInRestHistory() {
        int n = seq.incrementAndGet();
        String ownerCookie = registerAndLogin("flow2" + n + "@t.com", "flow2" + n);
        Long roomId = createRoom("FlowRoom2" + n, ownerCookie);
        Long senderId = getMyId(ownerCookie);

        messagePublisher.publishRoomMessage(new com.chatapp.message.dto.ChatMessageEvent(
                "ROOM", roomId, senderId, "flow2" + n, null, "REST check " + n, null));

        await().atMost(5, SECONDS).untilAsserted(() -> {
            ResponseEntity<MessagePage> res = get(
                    "/api/v1/rooms/" + roomId + "/messages", MessagePage.class, ownerCookie);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(res.getBody().messages()).hasSize(1);
            assertThat(res.getBody().messages().get(0).content()).isEqualTo("REST check " + n);
        });
    }

    // ---- helpers ----

    private Long createRoom(String name, String cookie) {
        ResponseEntity<RoomSummaryDto> res = post("/api/v1/rooms",
                new CreateRoomRequest(name, null, true), RoomSummaryDto.class, cookie);
        return res.getBody().id();
    }

    @SuppressWarnings("unchecked")
    private Long getMyId(String cookie) {
        ResponseEntity<java.util.Map<String, Object>> res =
                (ResponseEntity<java.util.Map<String, Object>>) (ResponseEntity<?>) get("/api/v1/users/me", java.util.Map.class, cookie);
        return Long.valueOf(res.getBody().get("id").toString());
    }

    private String registerAndLogin(String email, String username) {
        restTemplate.postForEntity("/api/v1/auth/register",
                new RegisterRequest(email, username, "password"), Void.class);
        return login(email);
    }

    private String login(String email) {
        ResponseEntity<Void> res = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(email, "password", false),
                Void.class);
        List<String> cookies = res.getHeaders().get(HttpHeaders.SET_COOKIE);
        return String.join("; ", cookies.stream().map(c -> c.split(";")[0]).toList());
    }

    private <T> ResponseEntity<T> post(String url, Object body, Class<T> type, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), type);
    }

    private <T> ResponseEntity<T> get(String url, Class<T> type, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), type);
    }
}
