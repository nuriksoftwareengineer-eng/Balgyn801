package com.nurba.java.config;

import com.nurba.java.service.delivery.provider.DeliveryProvider;
import com.nurba.java.service.delivery.provider.MockCdekProvider;
import com.nurba.java.service.delivery.provider.RealCdekProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Выбор активного {@link DeliveryProvider} по конфигурации {@code cdek.provider}:
 * <ul>
 *   <li>AUTO (по умолчанию) — real при заданных ключах, иначе mock;</li>
 *   <li>MOCK — всегда {@link MockCdekProvider};</li>
 *   <li>REAL — всегда {@link RealCdekProvider}.</li>
 * </ul>
 * Бизнес-логика инжектит {@link DeliveryProvider} и не знает, какая реализация активна.
 * Переключение mock↔real — только сменой ENV {@code CDEK_PROVIDER}/наличием ключей, без правок кода.
 */
@Slf4j
@Configuration
public class CdekProviderConfig {

    @Bean
    @Primary
    public DeliveryProvider deliveryProvider(
            CdekProperties props,
            MockCdekProvider mock,
            RealCdekProvider real
    ) {
        DeliveryProvider chosen = switch (props.resolveMode()) {
            case MOCK -> mock;
            case REAL -> real;
            case AUTO -> props.isConfigured() ? real : mock;
        };
        log.info("CDEK delivery provider: mode={}, active={}, configured={}",
                props.resolveMode(), chosen.name(), props.isConfigured());
        return chosen;
    }
}
