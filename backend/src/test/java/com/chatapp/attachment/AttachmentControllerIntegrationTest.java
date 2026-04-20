package com.chatapp.attachment;

import com.chatapp.attachment.dto.AttachmentDto;
import com.chatapp.auth.dto.LoginRequest;
import com.chatapp.auth.dto.RegisterRequest;
import com.chatapp.room.dto.CreateRoomRequest;
import com.chatapp.room.dto.RoomSummaryDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
                "app.jwt.secret=integration-test-secret-that-is-long-enough-for-hs256",
                "app.uploads.path=${java.io.tmpdir}/chatapp-test-uploads"
        }
)
class AttachmentControllerIntegrationTest {

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
    void upload_roomMember_returns200WithAttachmentDto() {
        int n = seq.incrementAndGet();
        String cookie = registerAndLogin("uploader" + n + "@t.com", "uploader" + n);
        Long roomId = createRoom("TestRoom" + n, cookie);

        ResponseEntity<AttachmentDto> res = uploadFile(roomId, null, cookie);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().filename()).isEqualTo("test.txt");
        assertThat(res.getBody().sizeBytes()).isGreaterThan(0);
        assertThat(res.getBody().messageId()).isNotNull();
    }

    @Test
    void upload_nonMember_returns403() {
        int n = seq.incrementAndGet();
        String owner = registerAndLogin("owner" + n + "@t.com", "owner" + n);
        String other = registerAndLogin("other" + n + "@t.com", "other" + n);
        Long roomId = createRoom("Room" + n, owner);

        ResponseEntity<Object> res = uploadFileRaw(roomId, null, other);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void download_nonMember_returns403() {
        int n = seq.incrementAndGet();
        String uploader = registerAndLogin("upl" + n + "@t.com", "upl" + n);
        String other = registerAndLogin("oth" + n + "@t.com", "oth" + n);
        Long roomId = createRoom("Rm" + n, uploader);

        AttachmentDto att = uploadFile(roomId, null, uploader).getBody();
        assertThat(att).isNotNull();

        HttpHeaders h = new HttpHeaders();
        h.add(HttpHeaders.COOKIE, other);
        ResponseEntity<Object> res = restTemplate.exchange(
                "/api/v1/attachments/" + att.id(), HttpMethod.GET,
                new HttpEntity<>(h), Object.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void download_member_returns200() {
        int n = seq.incrementAndGet();
        String cookie = registerAndLogin("dl" + n + "@t.com", "dl" + n);
        Long roomId = createRoom("DlRoom" + n, cookie);

        AttachmentDto att = uploadFile(roomId, null, cookie).getBody();
        assertThat(att).isNotNull();

        HttpHeaders h = new HttpHeaders();
        h.add(HttpHeaders.COOKIE, cookie);
        ResponseEntity<byte[]> res = restTemplate.exchange(
                "/api/v1/attachments/" + att.id(), HttpMethod.GET,
                new HttpEntity<>(h), byte[].class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getHeaders().getContentDisposition().getFilename()).isEqualTo("test.txt");
    }

    // ---- helpers ----

    private String registerAndLogin(String email, String username) {
        restTemplate.postForEntity("/api/v1/auth/register",
                new RegisterRequest(username, email, "password"), Void.class);
        ResponseEntity<Void> res = restTemplate.postForEntity("/api/v1/auth/login",
                new LoginRequest(email, "password", false), Void.class);
        List<String> cookies = res.getHeaders().get(HttpHeaders.SET_COOKIE);
        return String.join("; ", cookies.stream().map(c -> c.split(";")[0]).toList());
    }

    private Long createRoom(String name, String cookie) {
        HttpHeaders h = new HttpHeaders();
        h.add(HttpHeaders.COOKIE, cookie);
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<RoomSummaryDto> res = restTemplate.exchange(
                "/api/v1/rooms", HttpMethod.POST,
                new HttpEntity<CreateRoomRequest>(new CreateRoomRequest(name, null, false), h), RoomSummaryDto.class);
        return res.getBody().id();
    }

    private ResponseEntity<AttachmentDto> uploadFile(Long roomId, Long recipientId, String cookie) {
        HttpHeaders h = new HttpHeaders();
        h.add(HttpHeaders.COOKIE, cookie);
        h.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("hello world".getBytes()) {
            @Override public String getFilename() { return "test.txt"; }
        });
        if (roomId != null) body.add("roomId", roomId.toString());
        if (recipientId != null) body.add("recipientId", recipientId.toString());

        return restTemplate.exchange("/api/v1/attachments", HttpMethod.POST,
                new HttpEntity<>(body, h), AttachmentDto.class);
    }

    private ResponseEntity<Object> uploadFileRaw(Long roomId, Long recipientId, String cookie) {
        HttpHeaders h = new HttpHeaders();
        h.add(HttpHeaders.COOKIE, cookie);
        h.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("hello".getBytes()) {
            @Override public String getFilename() { return "test.txt"; }
        });
        if (roomId != null) body.add("roomId", roomId.toString());

        return restTemplate.exchange("/api/v1/attachments", HttpMethod.POST,
                new HttpEntity<>(body, h), Object.class);
    }
}
