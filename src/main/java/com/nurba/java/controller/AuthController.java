package com.nurba.java.controller;

import com.nurba.java.api.AuthApi;
import com.nurba.java.dto.request.AdminEmailRequest;
import com.nurba.java.dto.request.LoginRequest;
import com.nurba.java.dto.request.RefreshTokenRequest;
import com.nurba.java.dto.request.RegisterRequest;
import com.nurba.java.dto.responce.AuthMeResponse;
import com.nurba.java.dto.responce.AuthResponse;
import com.nurba.java.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        return authService.register(request);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        return authService.login(request);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        return authService.refresh(request);
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
}
