package com.chatapp.auth;

import com.chatapp.auth.dto.RegisterRequest;
import com.chatapp.auth.dto.ResetPasswordRequest;
import com.chatapp.auth.entity.PasswordResetToken;
import com.chatapp.auth.repository.PasswordResetTokenRepository;
import com.chatapp.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.actuate.autoconfigure.mail.MailHealthContributorAutoConfiguration",
                "app.jwt.secret=integration-test-secret-that-is-long-enough-for-hs256",
                "app.mail.from=noreply@chatapp.local",
                "app.frontend.url=http://localhost:3000"
        }
)
class PasswordResetControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean JavaMailSender mailSender;

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepo;
    @Autowired PasswordResetTokenRepository resetTokenRepo;

    private static final String EMAIL    = "bob@example.com";
    private static final String USERNAME = "bob";
    private static final String PASSWORD = "secret123";

    @BeforeEach
    void setUp() {
        resetTokenRepo.deleteAll();
        userRepo.deleteAll();
        restTemplate.postForEntity("/api/v1/auth/register",
                new RegisterRequest(EMAIL, USERNAME, PASSWORD), Void.class);
    }

    @Test
    void forgotPassword_unknownEmail_returns200() {
        ResponseEntity<Void> res = restTemplate.postForEntity(
                "/api/v1/auth/forgot-password",
                Map.of("email", "nobody@example.com"),
                Void.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(mailSender);
    }

    @Test
    void forgotPassword_knownEmail_sends200AndEmail() {
        ResponseEntity<Void> res = restTemplate.postForEntity(
                "/api/v1/auth/forgot-password",
                Map.of("email", EMAIL),
                Void.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void resetPassword_invalidToken_returns400() {
        ResponseEntity<Object> res = restTemplate.postForEntity(
                "/api/v1/auth/reset-password",
                new ResetPasswordRequest("bad-token", "newpassword"),
                Object.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void fullForgotResetFlow_tokenConsumed() {
        // trigger forgot-password to create token
        restTemplate.postForEntity("/api/v1/auth/forgot-password",
                Map.of("email", EMAIL), Void.class);

        PasswordResetToken prt = resetTokenRepo.findAll().get(0);
        String token = prt.getToken();

        // first use succeeds
        ResponseEntity<Void> reset = restTemplate.postForEntity(
                "/api/v1/auth/reset-password",
                new ResetPasswordRequest(token, "brandnewpassword"),
                Void.class);
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // second use is rejected
        ResponseEntity<Object> retry = restTemplate.postForEntity(
                "/api/v1/auth/reset-password",
                new ResetPasswordRequest(token, "anotherpassword"),
                Object.class);
        assertThat(retry.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // token is marked used
        PasswordResetToken updated = resetTokenRepo.findById(prt.getId()).orElseThrow();
        assertThat(updated.isUsed()).isTrue();
    }
}
