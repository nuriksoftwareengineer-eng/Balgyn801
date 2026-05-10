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

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(JwtProperties props) {
        byte[] keyBytes = props.secret().getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = props.expirationMs();
    }

    public String generateToken(AppUser user) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(expirationMs);
        String roles = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.joining(","));
        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("roles", roles)
                .signWith(signingKey)
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String expectedEmail) {
        try {
            String subject = extractEmail(token);
            Instant exp = parseClaims(token).getExpiration().toInstant();
            return expectedEmail.equalsIgnoreCase(subject) && exp.isAfter(Instant.now());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
