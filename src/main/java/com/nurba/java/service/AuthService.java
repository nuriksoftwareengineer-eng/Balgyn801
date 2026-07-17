package com.nurba.java.service;

import com.nurba.java.dto.request.LoginRequest;
import com.nurba.java.dto.request.RefreshTokenRequest;
import com.nurba.java.dto.request.RegisterRequest;
import com.nurba.java.dto.request.TelegramLoginRequest;
import com.nurba.java.dto.responce.AdminUserResponse;
import com.nurba.java.dto.responce.AuthMeResponse;
import com.nurba.java.dto.responce.AuthResponse;

import java.util.List;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    /** Вход через Telegram Mini App initData. Верифицирует подпись; находит существующего
     *  пользователя по telegram_id или создаёт нового (provider=TELEGRAM). */
    AuthResponse loginWithTelegram(TelegramLoginRequest request);

    /** Привязывает верифицированный Telegram-аккаунт к уже аутентифицированному пользователю.
     *  Отклоняется, если этот telegram_id уже привязан к другому аккаунту. */
    AuthMeResponse linkTelegram(String currentUserEmail, TelegramLoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    /** Обновить токены по сырому значению refresh-токена (из HttpOnly cookie). */
    AuthResponse refreshWithToken(String rawToken);

    AuthMeResponse me(String email);

    /** Идемпотентно выдаёт роль ADMIN. Возвращает актуальный набор ролей. */
    AuthMeResponse grantAdmin(String targetEmail);

    /**
     * Снимает роль ADMIN. Запрещено снимать у самого себя и у последнего админа,
     * чтобы система не осталась без доступа.
     */
    AuthMeResponse revokeAdmin(String currentAdminEmail, String targetEmail);

    /** Список всех зарегистрированных пользователей (только для ADMIN). */
    List<AdminUserResponse> listUsers();

    /** Инкрементирует token_version пользователя, инвалидируя все его refresh-токены. */
    void revokeRefreshTokens(String email);
}
