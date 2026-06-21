package com.nurba.java.controller;

import com.nurba.java.api.AuthApi;
import com.nurba.java.dto.request.AdminEmailRequest;
import com.nurba.java.dto.request.LoginRequest;
import com.nurba.java.dto.request.RefreshTokenRequest;
import com.nurba.java.dto.request.RegisterRequest;
import com.nurba.java.dto.responce.AuthMeResponse;
import com.nurba.java.dto.responce.AuthResponse;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.security.JwtProperties;
import com.nurba.java.security.JwtService;
import com.nurba.java.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final JwtService jwtService;

    @Value("${app.security.cookie-secure:false}")
    private boolean cookieSecure;

    @Override
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        AuthResponse auth = authService.register(request);
        setRefreshCookie(response, auth.getRefreshToken());
        return cookieResponse(auth);
    }

    @Override
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        AuthResponse auth = authService.login(request);
        setRefreshCookie(response, auth.getRefreshToken());
        return cookieResponse(auth);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @Override
    public AuthResponse refreshCookie(HttpServletRequest request, HttpServletResponse response) {
        String token = extractRefreshCookie(request);
        if (token == null || token.isBlank()) {
            throw new BusinessRuleException("Refresh-cookie отсутствует или просрочен");
        }
        AuthResponse auth = authService.refreshWithToken(token);
        setRefreshCookie(response, auth.getRefreshToken());
        return cookieResponse(auth);
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = extractRefreshCookie(request);
        if (rawToken != null && !rawToken.isBlank()) {
            try {
                String email = jwtService.extractEmail(rawToken);
                authService.revokeRefreshTokens(email);
            } catch (Exception ignored) {
                // Best-effort: always clear the cookie even if token is expired/invalid
            }
        }
        clearRefreshCookie(response);
    }

    @Override
    public AuthMeResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }

    @Override
    public AuthMeResponse grantAdmin(AdminEmailRequest request) {
        return authService.grantAdmin(request.getEmail());
    }

    @Override
    public AuthMeResponse revokeAdmin(AdminEmailRequest request, Authentication authentication) {
        return authService.revokeAdmin(authentication.getName(), request.getEmail());
    }

    /** Strip refreshToken from body — it travels via HttpOnly cookie only. */
    private static AuthResponse cookieResponse(AuthResponse auth) {
        auth.setRefreshToken(null);
        return auth;
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge((int) (jwtProperties.refreshExpirationMs() / 1000));
        cookie.setAttribute("SameSite", "Strict");
        if (cookieSecure) {
            cookie.setSecure(true);
        }
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private static String extractRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (REFRESH_COOKIE_NAME.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
