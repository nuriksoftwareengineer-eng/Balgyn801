package com.nurba.java.config;

import com.nurba.java.repositories.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Periodically drops expired refresh-token records so the {@code refresh_tokens} table stays small.
 * Rotation writes one row per refresh, so without this the table would grow with every session.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "${app.security.refresh-token-cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void purgeExpired() {
        int removed = refreshTokenRepository.deleteExpiredBefore(Instant.now());
        if (removed > 0) {
            log.info("[Auth] Purged {} expired refresh-token records", removed);
        }
    }
}
