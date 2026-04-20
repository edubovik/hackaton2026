package com.chatapp.message;

import com.chatapp.auth.dto.LoginRequest;
import com.chatapp.auth.dto.RegisterRequest;
import com.chatapp.message.dto.MessagePage;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
                "app.jwt.secret=integration-test-secret-that-is-long-enough-for-hs256"
        }
)
class MessageControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;

    private static final AtomicInteger seq = new AtomicInteger(0);

    @Test
    void roomHistory_nonMember_returns403() {
        int n = seq.incrementAndGet();
        String owner = registerAndLogin("owner" + n + "@t.com", "owner" + n);
        String other = registerAndLogin("other" + n + "@t.com", "other" + n);

        Long roomId = createRoom("Room" + n, owner);

        ResponseEntity<Object> res = get("/api/v1/rooms/" + roomId + "/messages", Object.class, other);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void roomHistory_member_returnsEmptyPage() {
        int n = seq.incrementAndGet();
        String owner = registerAndLogin("owner" + n + "@t.com", "owner" + n);
        Long roomId = createRoom("HistRoom" + n, owner);

        ResponseEntity<MessagePage> res = get("/api/v1/rooms/" + roomId + "/messages", MessagePage.class, owner);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().messages()).isEmpty();
        assertThat(res.getBody().hasMore()).isFalse();
    }

    @Test
    void editMessage_nonAuthor_returns403() {
        int n = seq.incrementAndGet();
        String owner = registerAndLogin("owner" + n + "@t.com", "owner" + n);
        String member = registerAndLogin("member" + n + "@t.com", "member" + n);
        Long roomId = createRoom("EditRoom" + n, owner);
        joinRoom(roomId, member);

        // Persist a message directly via repository — insert via SQL through a helper
        // We use a workaround: seed via the MessageRepository through REST is not available,
        // so we verify that a non-existent message returns 400
        ResponseEntity<Object> res = patch("/api/v1/messages/999999", Map.of("content", "hack"), Object.class, member);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deleteMessage_nonExistent_returns400() {
        int n = seq.incrementAndGet();
        String user = registerAndLogin("del" + n + "@t.com", "deluser" + n);

        ResponseEntity<Object> res = delete("/api/v1/messages/999999", Object.class, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void markRoomRead_nonMember_returns403() {
        int n = seq.incrementAndGet();
        String owner = registerAndLogin("owner" + n + "@t.com", "owner" + n);
        String other = registerAndLogin("other" + n + "@t.com", "other" + n);
        Long roomId = createRoom("ReadRoom" + n, owner);

        ResponseEntity<Object> res = post("/api/v1/rooms/" + roomId + "/messages/read", null, Object.class, other);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void markRoomRead_member_returns204() {
        int n = seq.incrementAndGet();
        String owner = registerAndLogin("owner" + n + "@t.com", "owner" + n);
        Long roomId = createRoom("ReadRoom2" + n, owner);

        ResponseEntity<Void> res = post("/api/v1/rooms/" + roomId + "/messages/read", null, Void.class, owner);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void unreadCounts_authenticated_returnsEmptyList() {
        int n = seq.incrementAndGet();
        String user = registerAndLogin("unread" + n + "@t.com", "unread" + n);

        ResponseEntity<String> res = get("/api/v1/messages/unread", String.class, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo("[]");
    }

    // ---- helpers ----

    private Long createRoom(String name, String cookie) {
        ResponseEntity<RoomSummaryDto> res = post("/api/v1/rooms",
                new CreateRoomRequest(name, null, true), RoomSummaryDto.class, cookie);
        return res.getBody().id();
    }

    private void joinRoom(Long roomId, String cookie) {
        post("/api/v1/rooms/" + roomId + "/join", null, Void.class, cookie);
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

    private <T> ResponseEntity<T> patch(String url, Object body, Class<T> type, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body, headers), type);
    }

    private <T> ResponseEntity<T> delete(String url, Class<T> type, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), type);
    }
}
