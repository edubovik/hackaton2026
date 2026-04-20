package com.chatapp.auth;

import com.chatapp.auth.dto.*;
import com.chatapp.auth.entity.User;
import com.chatapp.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;
    private final TokenCookieHelper cookieHelper;

    public AuthController(AuthService authService, TokenCookieHelper cookieHelper) {
        this.authService = authService;
        this.cookieHelper = cookieHelper;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest req,
                                       HttpServletRequest httpReq,
                                       HttpServletResponse httpRes) {
        AuthService.LoginResult result = authService.login(req, httpReq);
        cookieHelper.setAccessCookie(httpRes, result.accessToken());
        cookieHelper.setRefreshCookie(httpRes, result.refreshToken(), result.refreshTtlSeconds());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        String refreshToken = cookieHelper.readCookie(httpReq, TokenCookieHelper.REFRESH_COOKIE)
                .orElseThrow(() -> new UnauthorizedException("No refresh token"));
        String newAccessToken = authService.refresh(refreshToken);
        cookieHelper.setAccessCookie(httpRes, newAccessToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        cookieHelper.readCookie(httpReq, TokenCookieHelper.REFRESH_COOKIE)
                .ifPresent(authService::logout);
        cookieHelper.clearAuthCookies(httpRes);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/auth/sessions")
    public ResponseEntity<List<SessionResponse>> getSessions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.getSessions(user));
    }

    @DeleteMapping("/auth/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@AuthenticationPrincipal User user,
                                               @PathVariable Long id) {
        authService.deleteSession(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User user,
                                                @RequestBody ChangePasswordRequest req) {
        authService.changePassword(user, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user,
                                               @RequestBody DeleteAccountRequest req,
                                               HttpServletResponse httpRes) {
        authService.deleteAccount(user, req.password());
        cookieHelper.clearAuthCookies(httpRes);
        return ResponseEntity.noContent().build();
    }
}
