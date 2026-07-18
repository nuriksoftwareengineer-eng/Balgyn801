package com.nurba.java.security;

import org.springframework.web.client.RestClientResponseException;

/**
 * Telegram Bot API URLs embed the bot token ({@code /bot<token>/method}), and Spring's
 * transport-level exceptions (e.g. {@code ResourceAccessException}) include the full request
 * URL in their message — so logging {@code e.getMessage()} from a Telegram call can leak the
 * token into logs. This helper builds a description that is safe to log: HTTP status plus
 * Telegram's own response body for HTTP-level failures (Telegram never echoes the token in
 * bodies), or exception class names only for everything else.
 */
public final class TelegramApiErrors {

    private TelegramApiErrors() {
    }

    public static String describe(Exception e) {
        if (e instanceof RestClientResponseException re) {
            return "HTTP " + re.getStatusCode().value() + " — " + re.getResponseBodyAsString();
        }
        Throwable cause = e.getCause();
        return e.getClass().getSimpleName()
                + (cause != null ? " (" + cause.getClass().getSimpleName() + ")" : "");
    }
}
