package com.chatapp.auth;

import com.chatapp.auth.dto.LoginRequest;
import com.chatapp.auth.dto.RegisterRequest;
import com.chatapp.auth.entity.RefreshToken;
import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.RefreshTokenRepository;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepo;
    @Mock RefreshTokenRepository tokenRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock HttpServletRequest httpReq;

    @InjectMocks AuthService authService;

    @Test
    void register_duplicateEmail_throws() {
        when(userRepo.existsByEmail("a@b.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(new RegisterRequest("a@b.com", "alice", "pw")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void register_duplicateUsername_throws() {
        when(userRepo.existsByEmail(any())).thenReturn(false);
        when(userRepo.existsByUsername("alice")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(new RegisterRequest("a@b.com", "alice", "pw")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username");
    }

    @Test
    void register_success_savesUser() {
        when(userRepo.existsByEmail(any())).thenReturn(false);
        when(userRepo.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        User saved = new User();
        when(userRepo.save(any())).thenReturn(saved);

        User result = authService.register(new RegisterRequest("a@b.com", "alice", "pw"));
        assertThat(result).isNotNull();
        verify(passwordEncoder).encode("pw");
    }

    @Test
    void login_wrongPassword_throws() {
        User user = new User();
        user.setPassword("hashed");
        when(userRepo.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "wrong", false), httpReq))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_unknownEmail_throws() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("x@x.com", "pw", false), httpReq))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_expiredToken_throws() {
        RefreshToken rt = new RefreshToken();
        rt.setExpiresAt(OffsetDateTime.now().minusHours(1));
        User user = new User();
        rt.setUser(user);
        when(tokenRepo.findByToken("tok")).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.refresh("tok"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");
        verify(tokenRepo).delete(rt);
    }

    @Test
    void refresh_validToken_returnsNewAccessToken() {
        User user = new User();
        user.setUsername("alice");
        RefreshToken rt = new RefreshToken();
        rt.setExpiresAt(OffsetDateTime.now().plusDays(1));
        rt.setUser(user);
        when(tokenRepo.findByToken("tok")).thenReturn(Optional.of(rt));
        when(jwtService.generateAccessToken(any(), eq("alice"))).thenReturn("new-access");

        String result = authService.refresh("tok");
        assertThat(result).isEqualTo("new-access");
    }
}
