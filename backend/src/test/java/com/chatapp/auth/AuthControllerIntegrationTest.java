package com.chatapp.auth;

import com.chatapp.auth.dto.LoginRequest;
import com.chatapp.auth.dto.RegisterRequest;
import com.chatapp.auth.dto.SessionResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
                "app.jwt.secret=integration-test-secret-that-is-long-enough-for-hs256"
        }
)
class AuthControllerIntegrationTest {

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

    private static final String EMAIL    = "alice@example.com";
    private static final String USERNAME = "alice";
    private static final String PASSWORD = "secret123";

    @Test
    void fullRegisterLoginRefreshLogoutFlow() {
        // register
        ResponseEntity<Void> reg = restTemplate.postForEntity(
                "/api/v1/auth/register",
                new RegisterRequest(EMAIL, USERNAME, PASSWORD),
                Void.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // duplicate email → 400
        ResponseEntity<Object> dup = restTemplate.postForEntity(
                "/api/v1/auth/register",
                new RegisterRequest(EMAIL, "other", PASSWORD),
                Object.class);
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // login
        ResponseEntity<Void> login = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(EMAIL, PASSWORD, false),
                Void.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> setCookies = login.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).isNotNull().isNotEmpty();
        String cookieHeader = String.join("; ", setCookies.stream()
                .map(c -> c.split(";")[0])
                .toList());

        // sessions — authenticated
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookieHeader);
        ResponseEntity<SessionResponse[]> sessions = restTemplate.exchange(
                "/api/v1/auth/sessions",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                SessionResponse[].class);
        assertThat(sessions.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sessions.getBody()).hasSize(1);
        Long sessionId = sessions.getBody()[0].id();

        // refresh
        ResponseEntity<Void> refresh = restTemplate.exchange(
                "/api/v1/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Void.class);
        assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.OK);

        // delete session
        ResponseEntity<Void> delSession = restTemplate.exchange(
                "/api/v1/auth/sessions/" + sessionId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class);
        assertThat(delSession.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // logout
        ResponseEntity<Void> logout = restTemplate.exchange(
                "/api/v1/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Void.class);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void unauthenticatedAccess_returns401() {
        ResponseEntity<Object> res = restTemplate.getForEntity("/api/v1/auth/sessions", Object.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
