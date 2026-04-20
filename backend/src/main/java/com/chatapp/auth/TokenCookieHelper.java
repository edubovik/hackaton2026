package com.chatapp.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class TokenCookieHelper {

    public static final String ACCESS_COOKIE  = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";

    public void setAccessCookie(HttpServletResponse response, String token) {
        setCookie(response, ACCESS_COOKIE, token, 15 * 60);
    }

    public void setRefreshCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        setCookie(response, REFRESH_COOKIE, token, maxAgeSeconds);
    }

    public void clearAuthCookies(HttpServletResponse response) {
        setCookie(response, ACCESS_COOKIE, "", 0);
        setCookie(response, REFRESH_COOKIE, "", 0);
    }

    public Optional<String> readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        // Secure flag disabled for local dev; enable in prod via reverse proxy or profile override
        response.addCookie(cookie);
    }
}
