package com.nurba.java.service.Impl;

import com.nurba.java.domain.ExchangeRate;
import com.nurba.java.dto.responce.ExchangeRateResponse;
import com.nurba.java.dto.responce.PublicExchangeRatesResponse;
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
    private static final String CODE_KZT_EUR = "KZT_EUR";
    private static final String CODE_KZT_RUB = "KZT_RUB";
    private static final int RATE_SCALE = 4;

    /** Fallback KZT per 1 EUR when no DB row present. */
    @Value("${app.exchange-rate.kzt-per-eur-bootstrap:530.0000}")
    private BigDecimal bootstrapEur;

    /** Fallback KZT per 1 RUB when no DB row present. */
    @Value("${app.exchange-rate.kzt-per-rub-bootstrap:5.3000}")
    private BigDecimal bootstrapRub;

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
        return toResponse(upsert(CODE_KZT_USD, kztPerUsd, "MANUAL", frozen));
    }

    @Override
    @Transactional(readOnly = true)
    public PublicExchangeRatesResponse publicRates() {
        BigDecimal usd = repository.findById(CODE_KZT_USD)
                .map(ExchangeRate::getRate)
                .orElseGet(() -> bootstrap.setScale(RATE_SCALE, RoundingMode.HALF_UP));
        BigDecimal eur = repository.findById(CODE_KZT_EUR)
                .map(ExchangeRate::getRate)
                .orElseGet(() -> bootstrapEur.setScale(RATE_SCALE, RoundingMode.HALF_UP));
        BigDecimal rub = repository.findById(CODE_KZT_RUB)
                .map(ExchangeRate::getRate)
                .orElseGet(() -> bootstrapRub.setScale(RATE_SCALE, RoundingMode.HALF_UP));
        java.time.LocalDateTime updatedAt = repository.findById(CODE_KZT_USD)
                .map(ExchangeRate::getUpdatedAt)
                .orElse(null);
        return new PublicExchangeRatesResponse(usd, eur, rub, updatedAt);
    }

    /**
     * Best-effort hourly refresh of all KZT rates. Skips frozen USD rate; EUR/RUB refresh
     * continues even if USD is frozen. Checkout never triggers this.
     */
    @Override
    @Scheduled(cron = "${app.exchange-rate.refresh-cron:0 0 * * * *}")
    @Transactional
    public void refreshFromProvider() {
        ExchangeRate existing = repository.findById(CODE_KZT_USD).orElse(null);
        if (existing == null || !existing.isFrozen()) {
            provider.fetchKztPerUsd().ifPresentOrElse(
                    rate -> {
                        upsert(CODE_KZT_USD, rate, "AUTO", false);
                        log.info("Exchange rate refreshed: {} KZT/USD", rate);
                    },
                    () -> log.warn("Exchange rate provider unavailable for USD — keeping last known-good"));
        } else {
            log.debug("Exchange rate (USD) frozen — scheduled refresh skipped");
        }
        provider.fetchKztPerEur().ifPresent(rate -> {
            upsert(CODE_KZT_EUR, rate, "AUTO", false);
            log.info("Exchange rate refreshed: {} KZT/EUR", rate);
        });
        provider.fetchKztPerRub().ifPresent(rate -> {
            upsert(CODE_KZT_RUB, rate, "AUTO", false);
            log.info("Exchange rate refreshed: {} KZT/RUB", rate);
        });
    }

    private ExchangeRate upsert(String code, BigDecimal rate, String source, boolean frozen) {
        ExchangeRate entity = repository.findById(code).orElseGet(ExchangeRate::new);
        entity.setCode(code);
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
