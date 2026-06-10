package com.nurba.java.service.Impl;

import com.nurba.java.domain.ExchangeRate;
import com.nurba.java.dto.responce.ExchangeRateResponse;
import com.nurba.java.repositories.ExchangeRateRepository;
import com.nurba.java.service.ExchangeRateProvider;
import com.nurba.java.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {

    /** The only pair checkout needs: KZT per 1 USD. */
    public static final String CODE_KZT_USD = "KZT_USD";
    private static final int RATE_SCALE = 4;

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateServiceImpl.class);

    private final ExchangeRateRepository repository;
    private final ExchangeRateProvider provider;

    /** Cold-start fallback when no row exists (e.g. tests without Flyway). DB is authoritative. */
    @Value("${app.exchange-rate.kzt-per-usd-bootstrap:480.0000}")
    private BigDecimal bootstrap;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal kztPerUsd() {
        return repository.findById(CODE_KZT_USD)
                .map(ExchangeRate::getRate)
                .orElseGet(() -> bootstrap.setScale(RATE_SCALE, RoundingMode.HALF_UP));
    }

    @Override
    @Transactional(readOnly = true)
    public ExchangeRateResponse current() {
        return repository.findById(CODE_KZT_USD)
                .map(this::toResponse)
                .orElseGet(() -> new ExchangeRateResponse(
                        bootstrap.setScale(RATE_SCALE, RoundingMode.HALF_UP), "BOOTSTRAP", false, null));
    }

    @Override
    @Transactional
    public ExchangeRateResponse setManualRate(BigDecimal kztPerUsd, boolean frozen) {
        return toResponse(upsert(kztPerUsd, "MANUAL", frozen));
    }

    /**
     * Best-effort daily refresh. Skips entirely when the rate is frozen, and keeps the last
     * known-good value if the provider is unavailable. Checkout never triggers this.
     */
    @Override
    @Scheduled(cron = "${app.exchange-rate.refresh-cron:0 0 6 * * *}")
    @Transactional
    public void refreshFromProvider() {
        ExchangeRate existing = repository.findById(CODE_KZT_USD).orElse(null);
        if (existing != null && existing.isFrozen()) {
            log.debug("Exchange rate frozen — scheduled refresh skipped");
            return;
        }
        provider.fetchKztPerUsd().ifPresentOrElse(
                rate -> {
                    upsert(rate, "AUTO", false);
                    log.info("Exchange rate refreshed from provider: {} KZT/USD", rate);
                },
                () -> log.warn("Exchange rate provider unavailable — keeping last known-good value"));
    }

    private ExchangeRate upsert(BigDecimal rate, String source, boolean frozen) {
        ExchangeRate entity = repository.findById(CODE_KZT_USD).orElseGet(ExchangeRate::new);
        entity.setCode(CODE_KZT_USD);
        entity.setRate(rate.setScale(RATE_SCALE, RoundingMode.HALF_UP));
        entity.setSource(source);
        entity.setFrozen(frozen);
        entity.setUpdatedAt(LocalDateTime.now());
        return repository.save(entity);
    }

    private ExchangeRateResponse toResponse(ExchangeRate e) {
        return new ExchangeRateResponse(e.getRate(), e.getSource(), e.isFrozen(), e.getUpdatedAt());
    }
}
