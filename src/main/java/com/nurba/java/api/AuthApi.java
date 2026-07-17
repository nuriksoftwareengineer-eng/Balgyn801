package com.nurba.java.api;

import com.nurba.java.dto.request.AdminEmailRequest;
import com.nurba.java.dto.request.LoginRequest;
import com.nurba.java.dto.request.RefreshTokenRequest;
import com.nurba.java.dto.request.RegisterRequest;
import com.nurba.java.dto.request.TelegramLoginRequest;
import com.nurba.java.dto.responce.AuthMeResponse;
import com.nurba.java.dto.responce.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response);

    @Operation(summary = "Вход")
    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response);

    @Operation(summary = "Вход/регистрация через Telegram Mini App (initData)")
    @PostMapping("/telegram")
    AuthResponse loginTelegram(@Valid @RequestBody TelegramLoginRequest request, HttpServletResponse response);

    @Operation(
            summary = "Привязать Telegram-аккаунт к текущему пользователю",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/telegram/link")
    AuthMeResponse linkTelegram(@Valid @RequestBody TelegramLoginRequest request, Authentication authentication);

    @Operation(summary = "Обновить access-токен по refresh-токену из тела запроса")
    @PostMapping("/refresh")
    AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request);

    @Operation(summary = "Обновить access-токен по HttpOnly refresh-cookie (без тела)")
    @PostMapping("/refresh-cookie")
    AuthResponse refreshCookie(HttpServletRequest request, HttpServletResponse response);

    @Operation(summary = "Выход — очистить HttpOnly refresh-cookie и отозвать refresh-токен")
    @PostMapping("/logout")
    void logout(HttpServletRequest request, HttpServletResponse response);

    @Operation(summary = "Текущий пользователь по JWT")
    @GetMapping("/me")
    AuthMeResponse me(Authentication authentication);

    @Operation(
            summary = "Выдать роль ADMIN (только для ADMIN)",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/admin/grant")
    AuthMeResponse grantAdmin(@Valid @RequestBody AdminEmailRequest request);

    @Operation(
            summary = "Снять роль ADMIN (только для ADMIN; нельзя у себя и у последнего админа)",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/admin/revoke")
    AuthMeResponse revokeAdmin(
            @Valid @RequestBody AdminEmailRequest request,
            Authentication authentication);
}
