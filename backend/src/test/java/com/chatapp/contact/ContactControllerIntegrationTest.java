package com.chatapp.contact;

import com.chatapp.auth.dto.LoginRequest;
import com.chatapp.auth.dto.RegisterRequest;
import com.chatapp.contact.dto.FriendRequestDto;
import com.chatapp.contact.dto.SendFriendRequestDto;
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
class ContactControllerIntegrationTest {

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
    void fullRequestAcceptFlow() {
        int n = seq.incrementAndGet();
        String aliceCookie = registerAndLogin("alice" + n + "@t.com", "alice" + n);
        String bobCookie   = registerAndLogin("bob" + n + "@t.com",   "bob" + n);

        // Alice sends request to Bob
        ResponseEntity<FriendRequestDto> sent = post(
                "/api/v1/contacts/requests",
                new SendFriendRequestDto("bob" + n, "Hey!"),
                FriendRequestDto.class, aliceCookie);
        assertThat(sent.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long requestId = sent.getBody().id();

        // Bob sees incoming request
        ResponseEntity<FriendRequestDto[]> incoming = get(
                "/api/v1/contacts/requests/incoming", FriendRequestDto[].class, bobCookie);
        assertThat(incoming.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(incoming.getBody()).hasSize(1);
        assertThat(incoming.getBody()[0].fromUsername()).isEqualTo("alice" + n);

        // Bob accepts
        ResponseEntity<Void> accept = post(
                "/api/v1/contacts/requests/" + requestId + "/accept",
                null, Void.class, bobCookie);
        assertThat(accept.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Alice sees Bob in friend list
        ResponseEntity<Object[]> friends = get("/api/v1/contacts", Object[].class, aliceCookie);
        assertThat(friends.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(friends.getBody()).hasSize(1);
    }

    @Test
    void banUser_preventsFriendRequest() {
        int n = seq.incrementAndGet();
        String aliceCookie   = registerAndLogin("alice" + n + "@t.com",   "alice" + n);
        String charlieCookie = registerAndLogin("charlie" + n + "@t.com", "charlie" + n);

        // Need charlie's userId — get it from the friend list after making friends via a helper approach:
        // Send a request from charlie to alice, alice sees it and retrieves charlie's id from the request
        post("/api/v1/contacts/requests",
                new SendFriendRequestDto("alice" + n, null),
                FriendRequestDto.class, charlieCookie);
        ResponseEntity<FriendRequestDto[]> incoming = get(
                "/api/v1/contacts/requests/incoming", FriendRequestDto[].class, aliceCookie);
        Long charlieId = incoming.getBody()[0].fromUserId();

        // Charlie bans Alice
        post("/api/v1/contacts/" + charlieId + "/ban", null, Void.class, charlieCookie);

        // Alice tries to send request to Charlie → should be forbidden
        ResponseEntity<Object> req = post(
                "/api/v1/contacts/requests",
                new SendFriendRequestDto("charlie" + n, null),
                Object.class, aliceCookie);
        assertThat(req.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void banUser_terminatesExistingFriendship() {
        int n = seq.incrementAndGet();
        String aliceCookie = registerAndLogin("alice" + n + "@t.com", "alice" + n);
        String bobCookie   = registerAndLogin("bob" + n + "@t.com",   "bob" + n);

        // Alice and Bob become friends
        ResponseEntity<FriendRequestDto> sent = post(
                "/api/v1/contacts/requests",
                new SendFriendRequestDto("bob" + n, null),
                FriendRequestDto.class, aliceCookie);
        Long requestId = sent.getBody().id();
        post("/api/v1/contacts/requests/" + requestId + "/accept", null, Void.class, bobCookie);

        // Verify they are friends
        ResponseEntity<Map<String, Object>[]> friends = get("/api/v1/contacts", (Class<Map<String, Object>[]>) (Class<?>) Map[].class, aliceCookie);
        assertThat(friends.getBody()).hasSize(1);
        Long bobUserId = Long.valueOf(friends.getBody()[0].get("userId").toString());

        // Alice bans Bob
        post("/api/v1/contacts/" + bobUserId + "/ban", null, Void.class, aliceCookie);

        // Friend list should be empty
        ResponseEntity<Object[]> afterBan = get("/api/v1/contacts", Object[].class, aliceCookie);
        assertThat(afterBan.getBody()).isEmpty();
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
}
