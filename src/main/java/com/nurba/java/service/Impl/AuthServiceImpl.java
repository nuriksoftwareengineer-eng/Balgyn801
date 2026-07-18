package com.nurba.java.service.Impl;

import com.nurba.java.domain.AppUser;
import com.nurba.java.dto.request.LoginRequest;
import com.nurba.java.dto.request.RefreshTokenRequest;
import com.nurba.java.dto.request.RegisterRequest;
import com.nurba.java.dto.request.TelegramLoginRequest;
import com.nurba.java.dto.responce.AdminUserResponse;
import com.nurba.java.dto.responce.AuthMeResponse;
import com.nurba.java.dto.responce.AuthResponse;
import com.nurba.java.enums.AuthProvider;
import com.nurba.java.enums.Role;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.domain.RefreshTokenRecord;
import com.nurba.java.repositories.AppUserRepository;
import com.nurba.java.repositories.RefreshTokenRepository;
import com.nurba.java.security.JwtProperties;
import com.nurba.java.security.JwtService;
import com.nurba.java.security.TelegramInitDataVerifier;
import com.nurba.java.security.TelegramUserData;
import com.nurba.java.service.AuthService;
import com.nurba.java.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionRevoker sessionRevoker;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final TelegramInitDataVerifier telegramInitDataVerifier;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("Пользователь с таким email уже зарегистрирован");
        }

        AppUser user = AppUser.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(EnumSet.of(Role.USER))
                .createdAt(Instant.now())
                .build();

        appUserRepository.save(user);

        emailService.sendRegistrationEmail(user.getEmail(), user.getEmail());

        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (AuthenticationException ex) {
            throw new BusinessRuleException("Неверный email или пароль");
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse loginWithTelegram(TelegramLoginRequest request) {
        log.info("[Telegram] Login flow started");
        TelegramUserData tgUser = telegramInitDataVerifier.verify(request.getInitData());

        AppUser user = appUserRepository.findByTelegramId(tgUser.telegramId()).orElse(null);
        boolean isNewUser = user == null;
        log.info("[Telegram] {} for telegramId={}",
                isNewUser ? "Creating new user" : "Found existing user", tgUser.telegramId());
        if (isNewUser) {
            user = AppUser.builder()
                    .email("tg_" + tgUser.telegramId() + "@telegram.balgynbol.kz")
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .roles(EnumSet.of(Role.USER))
                    .createdAt(Instant.now())
                    .provider(AuthProvider.TELEGRAM)
                    .build();
        }
        applyTelegramFields(user, tgUser);
        appUserRepository.save(user);

        AuthResponse response = issueTokens(user);
        log.info("[Telegram] JWT issued for telegramId={}", tgUser.telegramId());
        return response;
    }

    @Override
    @Transactional
    public AuthMeResponse linkTelegram(String currentUserEmail, TelegramLoginRequest request) {
        log.info("[Telegram] Link flow started");
        TelegramUserData tgUser = telegramInitDataVerifier.verify(request.getInitData());

        AppUser currentUser = appUserRepository.findByEmailIgnoreCase(normalize(currentUserEmail))
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        appUserRepository.findByTelegramId(tgUser.telegramId()).ifPresent(existing -> {
            if (!existing.getId().equals(currentUser.getId())) {
                throw new BusinessRuleException("Этот Telegram-аккаунт уже привязан к другому пользователю");
            }
        });

        applyTelegramFields(currentUser, tgUser);
        appUserRepository.save(currentUser);
        log.info("[Telegram] Linked telegramId={} to existing account", tgUser.telegramId());
        return toMeResponse(currentUser);
    }

    private static AppUser applyTelegramFields(AppUser user, TelegramUserData tg) {
        user.setTelegramId(tg.telegramId());
        user.setTelegramUsername(tg.username());
        user.setTelegramFirstName(tg.firstName());
        user.setTelegramLastName(tg.lastName());
        user.setTelegramPhotoUrl(tg.photoUrl());
        return user;
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        return doRefreshWithToken(request.getRefreshToken().trim());
    }

    @Override
    @Transactional
    public AuthResponse refreshWithToken(String rawToken) {
        return doRefreshWithToken(rawToken.trim());
    }

    private AuthResponse doRefreshWithToken(String rawToken) {
        String email;
        try {
            email = jwtService.validateRefreshToken(rawToken);
        } catch (RuntimeException ex) {
            throw new BusinessRuleException("Невалидный или просроченный refresh-токен");
        }
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        // The refresh token must carry a jti backed by a server-side record. Tokens issued before
        // rotation existed have no jti/record and are rejected — the user simply re-logs in once.
        String jti = jwtService.extractJti(rawToken);
        RefreshTokenRecord record = (jti == null || jti.isBlank())
                ? null
                : refreshTokenRepository.findByJti(jti).orElse(null);

        if (record == null || !record.getUserId().equals(user.getId())) {
            throw new BusinessRuleException("Refresh-токен недействителен. Пожалуйста, войдите снова.");
        }
        if (record.isRevoked()) {
            // Reuse detection: a rotated/revoked token was replayed (possible theft) — revoke the
            // whole session family, then reject. The revoke runs in a NEW transaction so it commits
            // even though the BusinessRuleException below rolls this request's transaction back.
            sessionRevoker.revokeAllSessions(user.getId());
            log.warn("[Auth] Refresh-token reuse detected for user id={} — all sessions revoked", user.getId());
            throw new BusinessRuleException(
                    "Обнаружено повторное использование токена. Все сессии завершены — войдите снова.");
        }
        if (record.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Срок действия refresh-токена истёк");
        }

        // Rotate: revoke the presented token, issue a fresh access+refresh pair.
        record.setRevoked(true);
        refreshTokenRepository.save(record);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public void revokeRefreshTokens(String email) {
        if (email == null || email.isBlank()) return;
        appUserRepository.findByEmailIgnoreCase(email.trim().toLowerCase()).ifPresent(user -> {
            user.setTokenVersion(user.getTokenVersion() + 1);
            appUserRepository.save(user);
            refreshTokenRepository.revokeAllForUser(user.getId());
        });
    }

    /** Issues an access+refresh pair and records the refresh jti server-side (enables rotation). */
    private AuthResponse issueTokens(AppUser user) {
        String jti = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshTokenRecord.builder()
                .jti(jti)
                .userId(user.getId())
                .expiresAt(Instant.now().plusMillis(jwtProperties.refreshExpirationMs()))
                .revoked(false)
                .createdAt(Instant.now())
                .build());
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user, jti))
                .tokenType("Bearer")
                .expiresInMs(jwtProperties.expirationMs())
                .refreshExpiresInMs(jwtProperties.refreshExpirationMs())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthMeResponse me(String email) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        return toMeResponse(user);
    }

    @Override
    @Transactional
    public AuthMeResponse grantAdmin(String targetEmail) {
        String email = normalize(targetEmail);
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + email));

        if (!user.getRoles().contains(Role.ADMIN)) {
            user.getRoles().add(Role.ADMIN);
            appUserRepository.save(user);
        }
        return toMeResponse(user);
    }

    @Override
    @Transactional
    public AuthMeResponse revokeAdmin(String currentAdminEmail, String targetEmail) {
        String current = normalize(currentAdminEmail);
        String target = normalize(targetEmail);

        if (current.equalsIgnoreCase(target)) {
            throw new BusinessRuleException("Нельзя снять роль ADMIN у самого себя");
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(target)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + target));

        if (!user.getRoles().contains(Role.ADMIN)) {
            return toMeResponse(user);
        }

        long adminCount = appUserRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(Role.ADMIN))
                .count();
        if (adminCount <= 1) {
            throw new BusinessRuleException("Нельзя снять роль ADMIN у последнего администратора");
        }

        user.getRoles().remove(Role.ADMIN);
        appUserRepository.save(user);
        return toMeResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return appUserRepository.findAll().stream()
                .sorted(Comparator.comparing(AppUser::getId))
                .map(u -> AdminUserResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .roles(u.getRoles().stream().map(Role::name).collect(Collectors.toList()))
                        .createdAt(u.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static AuthMeResponse toMeResponse(AppUser user) {
        var roles = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toList());
        return AuthMeResponse.builder()
                .email(user.getEmail())
                .roles(roles)
                .telegramConnected(user.getTelegramId() != null)
                .telegramUsername(user.getTelegramUsername())
                .build();
    }
}
