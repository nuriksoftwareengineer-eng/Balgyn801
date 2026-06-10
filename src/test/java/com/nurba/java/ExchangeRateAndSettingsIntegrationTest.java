package com.nurba.java;

import com.nurba.java.dto.responce.ExchangeRateResponse;
import com.nurba.java.repositories.DeliverySettingRepository;
import com.nurba.java.repositories.ExchangeRateRepository;
import com.nurba.java.service.DeliverySettingService;
import com.nurba.java.service.ExchangeRateProvider;
import com.nurba.java.service.ExchangeRateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 4 — delivery settings and the exchange rate are DB-authoritative with cold-start
 * fallbacks. Checkout reads cached values; the scheduled refresh is best-effort and respects
 * freezing and provider outages.
 */
@SpringBootTest
@ActiveProfiles("test")
class ExchangeRateAndSettingsIntegrationTest {

    /** A controllable provider so the refresh paths are deterministic and never touch the network. */
    static class MutableProvider implements ExchangeRateProvider {
        volatile Optional<BigDecimal> value = Optional.of(new BigDecimal("500.0000"));
        @Override public Optional<BigDecimal> fetchKztPerUsd() { return value; }
    }

    @TestConfiguration
    static class StubConfig {
        @Bean @Primary MutableProvider mutableProvider() { return new MutableProvider(); }
    }

    @Autowired private DeliverySettingService deliverySettingService;
    @Autowired private ExchangeRateService exchangeRateService;
    @Autowired private DeliverySettingRepository deliverySettingRepository;
    @Autowired private ExchangeRateRepository exchangeRateRepository;
    @Autowired private MutableProvider provider;

    @BeforeEach
    void clean() {
        deliverySettingRepository.deleteAll();
        exchangeRateRepository.deleteAll();
        provider.value = Optional.of(new BigDecimal("500.0000"));
    }

    @AfterEach
    void tearDown() {
        // Don't leak settings/rates into the shared in-memory DB used by other test classes.
        deliverySettingRepository.deleteAll();
        exchangeRateRepository.deleteAll();
    }

    @Test
    void kzDeliveryFlat_fallsBackToBootstrap_thenReflectsAdminValue() {
        assertThat(deliverySettingService.kzDeliveryFlatKzt()).isEqualByComparingTo("1600");

        deliverySettingService.setKzDeliveryFlatKzt(new BigDecimal("2000"));
        assertThat(deliverySettingService.kzDeliveryFlatKzt()).isEqualByComparingTo("2000");
    }

    @Test
    void exchangeRate_fallsBackToBootstrap_whenNoRow() {
        assertThat(exchangeRateService.kztPerUsd()).isEqualByComparingTo("480.0000");
    }

    @Test
    void setManualRate_freezes_andScheduledRefreshDoesNotOverwrite() {
        exchangeRateService.setManualRate(new BigDecimal("460.0000"), true);

        provider.value = Optional.of(new BigDecimal("500.0000"));
        exchangeRateService.refreshFromProvider();   // frozen → skipped

        ExchangeRateResponse after = exchangeRateService.current();
        assertThat(after.kztPerUsd()).isEqualByComparingTo("460.0000");
        assertThat(after.source()).isEqualTo("MANUAL");
        assertThat(after.frozen()).isTrue();
    }

    @Test
    void scheduledRefresh_updatesWhenNotFrozen() {
        exchangeRateService.setManualRate(new BigDecimal("460.0000"), false);

        provider.value = Optional.of(new BigDecimal("500.0000"));
        exchangeRateService.refreshFromProvider();

        ExchangeRateResponse after = exchangeRateService.current();
        assertThat(after.kztPerUsd()).isEqualByComparingTo("500.0000");
        assertThat(after.source()).isEqualTo("AUTO");
    }

    @Test
    void scheduledRefresh_keepsLastKnownGood_whenProviderUnavailable() {
        exchangeRateService.setManualRate(new BigDecimal("470.0000"), false);

        provider.value = Optional.empty();            // provider down
        exchangeRateService.refreshFromProvider();

        assertThat(exchangeRateService.kztPerUsd()).isEqualByComparingTo("470.0000");
    }
}
