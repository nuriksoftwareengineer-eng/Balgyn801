package com.nurba.java.service.Impl;

import com.nurba.java.enums.TariffKind;
import com.nurba.java.service.ExchangeRateService;
import com.nurba.java.service.ShippingTariffService;
import com.nurba.java.service.delivery.InternationalShippingQuote;
import com.nurba.java.service.delivery.InternationalShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class InternationalShippingServiceImpl implements InternationalShippingService {

    private final ShippingTariffService tariffService;
    private final ExchangeRateService exchangeRateService;

    /** Fixed USD markup added to every international shipment, converted at the cached rate. */
    @Value("${app.exchange-rate.markup-usd:5}")
    private BigDecimal markupUsd;

    @Override
    public InternationalShippingQuote quote(BigDecimal weightKg) {
        // AIR tariff base (KZT) for the weight bracket — always AIR, never surfaced to the customer.
        BigDecimal baseKzt = tariffService.baseFeeKzt(TariffKind.AIR, weightKg);

        // Read the cached rate (never a live API call), so checkout works even if the provider is down.
        BigDecimal rate = exchangeRateService.kztPerUsd();

        BigDecimal markupKzt = markupUsd.multiply(rate);
        BigDecimal feeKzt = baseKzt.add(markupKzt).setScale(2, RoundingMode.HALF_UP);
        BigDecimal feeUsd = baseKzt.divide(rate, 2, RoundingMode.HALF_UP)
                .add(markupUsd)
                .setScale(2, RoundingMode.HALF_UP);

        return new InternationalShippingQuote(feeKzt, feeUsd, rate.setScale(4, RoundingMode.HALF_UP));
    }
}
