package com.nurba.java.api;

import com.nurba.java.dto.request.LoginRequest;
import com.nurba.java.dto.request.RegisterRequest;
import com.nurba.java.dto.responce.AuthMeResponse;
import com.nurba.java.dto.responce.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Auth", description = "Регистрация и вход (JWT)")
@RequestMapping("/api/v1/auth")
public interface AuthApi {

    @Operation(summary = "Регистрация")
    @PostMapping("/register")
    AuthResponse register(@Valid @RequestBody RegisterRequest request);

    @Operation(summary = "Вход")
    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request);

    @Operation(summary = "Текущий пользователь по JWT")
    @GetMapping("/me")
    AuthMeResponse me(Authentication authentication);
}
