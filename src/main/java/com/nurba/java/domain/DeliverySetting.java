package com.nurba.java.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Admin-editable numeric delivery setting (key/value).
 * <p>
 * Lets domestic pricing change without a redeploy — e.g. the Kazakhstan flat rate lives here under
 * {@code KZ_DELIVERY_FLAT_KZT}. New settings are added as new rows, not schema changes.
 */
@Entity
@Table(name = "delivery_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliverySetting {

    @Id
    @Column(name = "setting_key", length = 64)
    private String settingKey;

    @Column(name = "numeric_value", precision = 12, scale = 2, nullable = false)
    private BigDecimal numericValue;
}
