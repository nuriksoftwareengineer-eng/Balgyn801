package com.nurba.java.service.Impl;

import com.nurba.java.domain.AppUser;
import com.nurba.java.dto.request.LoginRequest;
import com.nurba.java.dto.request.RefreshTokenRequest;
import com.nurba.java.dto.request.RegisterRequest;
import com.nurba.java.dto.responce.AuthMeResponse;
import com.nurba.java.dto.responce.AuthResponse;
import com.nurba.java.enums.Role;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.repositories.AppUserRepository;
import com.nurba.java.security.JwtProperties;
import com.nurba.java.security.JwtService;
import com.nurba.java.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;

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

        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
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

        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String email;
        try {
            email = jwtService.validateRefreshToken(request.getRefreshToken().trim());
        } catch (RuntimeException ex) {
            throw new BusinessRuleException("Невалидный или просроченный refresh-токен");
        }
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshWithToken(String rawToken) {
        String email;
        try {
            email = jwtService.validateRefreshToken(rawToken.trim());
        } catch (RuntimeException ex) {
            throw new BusinessRuleException("Невалидный или просроченный refresh-токен");
        }
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(AppUser user) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
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
                .build();
    }
}
