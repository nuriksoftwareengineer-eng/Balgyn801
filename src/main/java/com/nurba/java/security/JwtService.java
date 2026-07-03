package com.nurba.java.security;

import com.nurba.java.domain.AppUser;
import com.nurba.java.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final String CLAIM_TYP = "typ";
    private static final String TYP_ACCESS = "access";
    private static final String TYP_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(JwtProperties props) {
        String secret = props.secret() == null ? "" : props.secret();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET не задан или слишком короткий (" + keyBytes.length + " байт). "
                            + "HS256 требует минимум 32 байта. Скопируйте .env.example в .env "
                            + "(cp .env.example .env) или задайте переменную окружения JWT_SECRET.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpirationMs = props.expirationMs();
        this.refreshExpirationMs = props.refreshExpirationMs();
    }

    public String generateAccessToken(AppUser user) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(accessExpirationMs);
        String roles = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.joining(","));
        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim(CLAIM_TYP, TYP_ACCESS)
                .claim("roles", roles)
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(AppUser user, String jti) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(refreshExpirationMs);
        return Jwts.builder()
                .id(jti)                       // standard "jti" claim — ties the token to its server-side record
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim(CLAIM_TYP, TYP_REFRESH)
                .claim("ver", user.getTokenVersion())
                .signWith(signingKey)
                .compact();
    }

    /** Returns the jti (token id) of a refresh token, or null if absent/unparseable. */
    public String extractJti(String token) {
        try {
            return parseClaims(token).getId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** Returns the token_version embedded in the refresh token, or -1 if absent (legacy token). */
    public int extractRefreshVersion(String token) {
        try {
            Claims claims = parseClaims(token);
            Object ver = claims.get("ver");
            if (ver instanceof Number n) return n.intValue();
        } catch (RuntimeException ignored) { }
        return -1;
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Допускает только access-токен (или старые токены без claim {@code typ} — для плавного перехода).
     */
    public boolean isAccessTokenValid(String token, String expectedEmail) {
        try {
            Claims claims = parseClaims(token);
            String typ = claims.get(CLAIM_TYP, String.class);
            if (TYP_REFRESH.equalsIgnoreCase(typ)) {
                return false;
            }
            String subject = claims.getSubject();
            Instant exp = claims.getExpiration().toInstant();
            return expectedEmail.equalsIgnoreCase(subject) && exp.isAfter(Instant.now());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** Проверяет refresh-токен и возвращает email субъекта. */
    public String validateRefreshToken(String token) {
        Claims claims = parseClaims(token);
        String typ = claims.get(CLAIM_TYP, String.class);
        if (!TYP_REFRESH.equalsIgnoreCase(typ)) {
            throw new IllegalArgumentException("Неверный тип токена");
        }
        Instant exp = claims.getExpiration().toInstant();
        if (!exp.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Срок действия refresh-токена истёк");
        }
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Неверный refresh-токен");
        }
        return subject.trim().toLowerCase();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
