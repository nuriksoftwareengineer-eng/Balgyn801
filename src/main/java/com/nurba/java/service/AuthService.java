package com.nurba.java.service;

import com.nurba.java.dto.request.LoginRequest;
import com.nurba.java.dto.request.RefreshTokenRequest;
import com.nurba.java.dto.request.RegisterRequest;
import com.nurba.java.dto.responce.AuthMeResponse;
import com.nurba.java.dto.responce.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

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
}
