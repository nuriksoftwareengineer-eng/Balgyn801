package com.nurba.java.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Loud startup warnings for insecure DEV defaults. Does not block startup — dev must stay
 * zero-config ({@code docker-compose.yml} ships working defaults) — but makes it impossible to
 * miss that the running configuration is NOT production-safe.
 *
 * <p>The production profile ({@code docker-compose.prod.yml}) supplies every secret via
 * {@code ${VAR:?}} (fails to start if missing), so none of these warnings fire in prod.
 */
@Slf4j
@Component
public class InsecureConfigWarner {

    /** Public dev-only values baked into docker-compose.yml — safe to hardcode here (already public). */
    private static final String DEV_JWT_SECRET =
            "balgyn-dev-jwt-secret-for-local-development-only-change-in-prod";
    private static final String DEV_ADMIN_PASSWORD = "admin12345";

    private final Environment env;
    private final String jwtSecret;
    private final String adminPassword;
    private final String cdekWebhookToken;

    public InsecureConfigWarner(
            Environment env,
            @Value("${jwt.secret:}") String jwtSecret,
            @Value("${app.security.bootstrap-admin-password:}") String adminPassword,
            @Value("${cdek.webhook-token:}") String cdekWebhookToken) {
        this.env = env;
        this.jwtSecret = jwtSecret;
        this.adminPassword = adminPassword;
        this.cdekWebhookToken = cdekWebhookToken;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warnOnInsecureDefaults() {
        boolean prod = Arrays.asList(env.getActiveProfiles()).contains("prod");
        boolean devJwt = DEV_JWT_SECRET.equals(jwtSecret);

        if (devJwt) {
            log.warn("==================================================================================");
            log.warn("SECURITY WARNING: JWT_SECRET is the PUBLIC dev default from docker-compose.yml.");
            log.warn("  Anyone with the repo can forge ADMIN tokens. Never expose this build publicly.");
            log.warn("  Set a unique JWT_SECRET (openssl rand -hex 32) before any real deployment.");
            log.warn("==================================================================================");
        }
        if (DEV_ADMIN_PASSWORD.equals(adminPassword)) {
            log.warn("SECURITY WARNING: BOOTSTRAP_ADMIN_PASSWORD is the public dev default 'admin12345' — "
                    + "change it before any public deployment.");
        }
        if (prod && (cdekWebhookToken == null || cdekWebhookToken.isBlank())) {
            log.warn("SECURITY WARNING: prod profile active but CDEK_WEBHOOK_TOKEN is blank — "
                    + "CDEK webhooks would be accepted UNSIGNED. Set CDEK_WEBHOOK_TOKEN.");
        }
        if (prod && devJwt) {
            log.error("SECURITY FATAL: production profile is running with the PUBLIC dev JWT secret. "
                    + "Rotate JWT_SECRET immediately — all issued tokens are forgeable.");
        }
    }
}
