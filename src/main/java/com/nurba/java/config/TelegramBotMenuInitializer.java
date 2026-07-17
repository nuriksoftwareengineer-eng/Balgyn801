package com.nurba.java.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Sets the bot's persistent Menu Button once on startup so tapping it opens the Mini App —
 * Telegram's documented way to launch a Mini App from a bot chat (not a live webhook/command
 * handler). Idempotent: re-running setChatMenuButton on every restart just re-applies the
 * same config.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.telegram.enabled", havingValue = "true")
public class TelegramBotMenuInitializer {

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    private final RestClient restClient = RestClient.create();

    @PostConstruct
    public void setMenuButton() {
        if (botToken == null || botToken.isBlank()) return;
        if (!frontendBaseUrl.startsWith("https://")) {
            // Telegram requires an HTTPS url for web_app menu buttons — expected in local dev.
            log.warn("[Telegram] app.frontend.base-url={} не HTTPS — пропускаю setChatMenuButton", frontendBaseUrl);
            return;
        }
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/setChatMenuButton";
            restClient.post()
                    .uri(url)
                    .body(Map.of("menu_button", Map.of(
                            "type", "web_app",
                            "text", "Открыть магазин",
                            "web_app", Map.of("url", frontendBaseUrl))))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Telegram] Bot menu button configured to open {}", frontendBaseUrl);
        } catch (Exception e) {
            log.warn("[Telegram] Failed to set bot menu button: {}", e.getMessage());
        }
    }
}
