package com.nurba.java.service;

import com.nurba.java.enums.TariffKind;

import java.math.BigDecimal;

/**
 * Resolves the base shipping fee (KZT) for a weight from an admin-editable bracket table.
 * Backend-only; the bracket structure is never exposed to the customer.
 */
public interface ShippingTariffService {

    /**
     * @return base fee (KZT) for the smallest bracket whose upper bound covers {@code weightKg}
     * @throws com.nurba.java.exception.BusinessRuleException if the weight exceeds all brackets
     */
    BigDecimal baseFeeKzt(TariffKind kind, BigDecimal weightKg);
}
