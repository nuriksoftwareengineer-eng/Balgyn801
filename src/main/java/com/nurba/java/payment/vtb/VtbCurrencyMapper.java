package com.nurba.java.payment.vtb;

import com.nurba.java.config.VtbProperties;
import com.nurba.java.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class VtbCurrencyMapper {

    private static final Map<String, Integer> ISO_NUMERIC = Map.of(
            "KZT", 398,
            "RUB", 643,
            "USD", 840,
            "EUR", 978
    );

    /**
     * Maps a currency string to an ISO 4217 numeric code, checking merchant support.
     * Throws BusinessRuleException if the currency is unsupported and fallback is disabled.
     */
    public int mapCurrency(String currency, VtbProperties props) {
        if (currency == null || currency.isBlank()) {
            currency = "KZT";
        }
        Integer isoCode = ISO_NUMERIC.get(currency.toUpperCase());
        if (isoCode == null) {
            throw new BusinessRuleException("Unknown currency for VTB KZ: " + currency);
        }
        if (props.getSupportedCurrencyCodes().contains(isoCode)) {
            return isoCode;
        }
        if (props.isFallbackToKzt()) {
            return 398;
        }
        throw new BusinessRuleException(
                "Currency " + currency + " (ISO " + isoCode
                + ") is not supported by VTB KZ merchant configuration. "
                + "Supported: " + props.getSupportedCurrencies());
    }

    /** Converts standard currency units to minor units (×100). */
    public static long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
