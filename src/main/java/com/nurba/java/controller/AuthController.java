package com.nurba.java.controller;

import com.nurba.java.api.AuthApi;
import com.nurba.java.dto.request.LoginRequest;
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
    public AuthMeResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
