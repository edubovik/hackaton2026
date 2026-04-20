package com.chatapp.auth;

import com.chatapp.auth.dto.*;
import com.chatapp.auth.entity.RefreshToken;
import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.RefreshTokenRepository;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final int SESSION_TTL_SECONDS    = 24 * 60 * 60;
    private static final int PERSISTENT_TTL_SECONDS = 30 * 24 * 60 * 60;

    private final UserRepository userRepo;
    private final RefreshTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepo, RefreshTokenRepository tokenRepo,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            throw new BadRequestException("Email already in use");
        }
        if (userRepo.existsByUsername(req.username())) {
            throw new BadRequestException("Username already taken");
        }
        User user = new User();
        user.setEmail(req.email());
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        return userRepo.save(user);
    }

    public LoginResult login(LoginRequest req, HttpServletRequest httpReq) {
        User user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());
        String rawRefreshToken = UUID.randomUUID().toString();
        int ttlSeconds = req.keepMeSignedIn() ? PERSISTENT_TTL_SECONDS : SESSION_TTL_SECONDS;

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(rawRefreshToken);
        rt.setUserAgent(httpReq.getHeader("User-Agent"));
        rt.setIpAddress(extractIp(httpReq));
        rt.setExpiresAt(OffsetDateTime.now().plusSeconds(ttlSeconds));
        tokenRepo.save(rt);

        return new LoginResult(accessToken, rawRefreshToken, ttlSeconds);
    }

    public String refresh(String rawRefreshToken) {
        RefreshToken rt = tokenRepo.findByToken(rawRefreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (rt.getExpiresAt().isBefore(OffsetDateTime.now())) {
            tokenRepo.delete(rt);
            throw new UnauthorizedException("Refresh token expired");
        }
        return jwtService.generateAccessToken(rt.getUser().getId(), rt.getUser().getUsername());
    }

    public void logout(String rawRefreshToken) {
        tokenRepo.deleteByToken(rawRefreshToken);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getSessions(User user) {
        return tokenRepo.findByUser(user).stream()
                .map(rt -> new SessionResponse(rt.getId(), rt.getUserAgent(),
                        rt.getIpAddress(), rt.getCreatedAt(), rt.getExpiresAt()))
                .toList();
    }

    public void deleteSession(User user, Long sessionId) {
        RefreshToken rt = tokenRepo.findById(sessionId)
                .orElseThrow(() -> new BadRequestException("Session not found"));
        if (!rt.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not your session");
        }
        tokenRepo.delete(rt);
    }

    public void changePassword(User user, ChangePasswordRequest req) {
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepo.save(user);
    }

    public void deleteAccount(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Incorrect password");
        }
        userRepo.delete(user);
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record LoginResult(String accessToken, String refreshToken, int refreshTtlSeconds) {}
}
