package com.chatapp.room;

import com.chatapp.auth.dto.LoginRequest;
import com.chatapp.auth.dto.RegisterRequest;
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
class RoomControllerIntegrationTest {

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
    void createJoinLeaveDeleteLifecycle() {
        int n = seq.incrementAndGet();
        String ownerCookie = registerAndLogin("owner" + n + "@t.com", "owner" + n);
        String memberCookie = registerAndLogin("member" + n + "@t.com", "member" + n);

        // Create room
        ResponseEntity<RoomSummaryDto> created = post(
                "/api/v1/rooms",
                new CreateRoomRequest("Room" + n, "desc", true),
                RoomSummaryDto.class, ownerCookie);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long roomId = created.getBody().id();

        // Member joins
        ResponseEntity<Void> join = post("/api/v1/rooms/" + roomId + "/join", null, Void.class, memberCookie);
        assertThat(join.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Owner sees both members
        ResponseEntity<Object[]> members = get("/api/v1/rooms/" + roomId + "/members", Object[].class, ownerCookie);
        assertThat(members.getBody()).hasSize(2);

        // Member leaves
        ResponseEntity<Void> leave = delete("/api/v1/rooms/" + roomId + "/leave", Void.class, memberCookie);
        assertThat(leave.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Owner deletes room
        ResponseEntity<Void> deleted = delete("/api/v1/rooms/" + roomId, Void.class, ownerCookie);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void banPreventsRejoin() {
        int n = seq.incrementAndGet();
        String ownerCookie = registerAndLogin("owner" + n + "@t.com", "owner" + n);
        String memberCookie = registerAndLogin("member" + n + "@t.com", "member" + n);

        ResponseEntity<RoomSummaryDto> created = post(
                "/api/v1/rooms",
                new CreateRoomRequest("BanRoom" + n, null, true),
                RoomSummaryDto.class, ownerCookie);
        Long roomId = created.getBody().id();

        // Member joins
        post("/api/v1/rooms/" + roomId + "/join", null, Void.class, memberCookie);

        // Get member's userId
        ResponseEntity<Object[]> members = get("/api/v1/rooms/" + roomId + "/members", Object[].class, ownerCookie);
        @SuppressWarnings("unchecked")
        Map<String, Object> memberEntry = (Map<String, Object>) members.getBody()[1];
        Long memberId = Long.valueOf(memberEntry.get("userId").toString());

        // Owner bans member
        ResponseEntity<Void> ban = post(
                "/api/v1/rooms/" + roomId + "/members/" + memberId + "/ban",
                null, Void.class, ownerCookie);
        assertThat(ban.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Banned member tries to rejoin
        ResponseEntity<Object> rejoin = post("/api/v1/rooms/" + roomId + "/join", null, Object.class, memberCookie);
        assertThat(rejoin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void ownerCannotLeaveRoom() {
        int n = seq.incrementAndGet();
        String ownerCookie = registerAndLogin("owner" + n + "@t.com", "owner" + n);

        ResponseEntity<RoomSummaryDto> created = post(
                "/api/v1/rooms",
                new CreateRoomRequest("OwnerRoom" + n, null, true),
                RoomSummaryDto.class, ownerCookie);
        Long roomId = created.getBody().id();

        ResponseEntity<Object> leave = delete("/api/v1/rooms/" + roomId + "/leave", Object.class, ownerCookie);
        assertThat(leave.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void privateRoomNotInCatalog() {
        int n = seq.incrementAndGet();
        String ownerCookie = registerAndLogin("owner" + n + "@t.com", "owner" + n);

        post("/api/v1/rooms",
                new CreateRoomRequest("PrivRoom" + n, null, false),
                RoomSummaryDto.class, ownerCookie);

        ResponseEntity<Object> catalog = get("/api/v1/rooms?search=PrivRoom" + n, Object.class, ownerCookie);
        assertThat(catalog.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<?> content = (List<?>) ((Map<String, Object>) catalog.getBody()).get("content");
        assertThat(content).isEmpty();
    }

    // ---- helpers ----

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

    private <T> ResponseEntity<T> delete(String url, Class<T> type, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), type);
    }
}
