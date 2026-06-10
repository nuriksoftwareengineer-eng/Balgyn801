package com.nurba.java.service;

import java.math.BigDecimal;

/**
 * Reads and updates admin-editable delivery settings (DB-authoritative, with a cold-start fallback).
 */
public interface DeliverySettingService {

    /** Flat Kazakhstan delivery fee in KZT (same for every domestic destination). */
    BigDecimal kzDeliveryFlatKzt();

    /** Admin: set the flat Kazakhstan delivery fee. */
    BigDecimal setKzDeliveryFlatKzt(BigDecimal value);
}
