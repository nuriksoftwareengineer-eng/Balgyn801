package com.nurba.java.service.Impl;

import com.nurba.java.domain.AppUser;
import com.nurba.java.dto.request.LoginRequest;
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

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(jwtProperties.expirationMs())
                .build();
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

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(jwtProperties.expirationMs())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthMeResponse me(String email) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        var roles = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toList());
        return AuthMeResponse.builder()
                .email(user.getEmail())
                .roles(roles)
                .build();
    }
}
