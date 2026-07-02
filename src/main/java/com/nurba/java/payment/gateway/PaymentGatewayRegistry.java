package com.nurba.java.payment.gateway;

import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Auto-registers all PaymentGateway @Service beans via Spring DI.
 * Adding a new provider = create a @Service that implements PaymentGateway; no other config needed.
 */
@Slf4j
@Component
public class PaymentGatewayRegistry {

    private final Map<PaymentProvider, PaymentGateway> gateways;

    public PaymentGatewayRegistry(List<PaymentGateway> allGateways) {
        this.gateways = allGateways.stream()
                .collect(Collectors.toUnmodifiableMap(
                        PaymentGateway::getProvider,
                        Function.identity()
                ));
        log.info("[PaymentGatewayRegistry] Registered {} gateway(s): {}",
                gateways.size(), gateways.keySet());
    }

    public PaymentGateway get(PaymentProvider provider) {
        return Optional.ofNullable(gateways.get(provider))
                .orElseThrow(() -> new BusinessRuleException(
                        "No payment gateway registered for provider: " + provider));
    }
}
