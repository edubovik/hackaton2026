package com.chatapp.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("test-secret-key-that-is-long-enough-for-hs256");
    }

    @Test
    void generateAndParseToken() {
        String token = jwtService.generateAccessToken(42L, "alice");
        Claims claims = jwtService.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("username")).isEqualTo("alice");
    }

    @Test
    void extractUserId() {
        String token = jwtService.generateAccessToken(7L, "bob");
        assertThat(jwtService.extractUserId(token)).isEqualTo(7L);
    }

    @Test
    void isTokenValid_trueForValidToken() {
        String token = jwtService.generateAccessToken(1L, "user");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_falseForGarbage() {
        assertThat(jwtService.isTokenValid("not.a.token")).isFalse();
    }

    @Test
    void isTokenValid_falseForWrongSignature() {
        JwtService other = new JwtService("completely-different-secret-that-is-also-long-enough");
        String token = other.generateAccessToken(1L, "user");
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
