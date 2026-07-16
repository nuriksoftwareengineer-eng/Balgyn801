package com.nurba.java.dto.delivery;

import java.math.BigDecimal;

/** Стоимость международной доставки для выбранной страны и типа перевозки. */
public record IntlQuoteResponse(BigDecimal priceKzt, BigDecimal priceUsd) {
}
