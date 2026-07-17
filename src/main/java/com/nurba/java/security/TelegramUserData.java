package com.nurba.java.security;

/** Данные пользователя, извлечённые из проверенного Telegram initData. */
public record TelegramUserData(
        long telegramId,
        String username,
        String firstName,
        String lastName,
        String photoUrl
) {
}
