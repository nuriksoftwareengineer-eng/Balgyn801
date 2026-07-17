package com.nurba.java.enums;

/** Как аккаунт был изначально создан. Не гейт для входа — LOCAL-пользователь может
 *  позже привязать Telegram (см. AuthService.linkTelegram) без смены provider. */
public enum AuthProvider {
    LOCAL,
    TELEGRAM
}
