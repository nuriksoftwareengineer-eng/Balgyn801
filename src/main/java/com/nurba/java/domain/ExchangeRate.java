package com.nurba.java.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cached currency rate. The only row used by checkout is {@code KZT_USD} = KZT per 1 USD.
 * <p>
 * Checkout reads this row and never calls an external API inline, so orders place fine even when
 * the rate provider is unavailable. A scheduled job refreshes it best-effort (keeping the last
 * known-good value on failure); admin can override and {@link #frozen freeze} it.
 */
@Entity
@Table(name = "exchange_rates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRate {

    /** Pair code, e.g. "KZT_USD" (KZT per 1 USD). */
    @Id
    @Column(name = "code", length = 16)
    private String code;

    @Column(name = "rate", precision = 14, scale = 4, nullable = false)
    private BigDecimal rate;

    /** Where the current value came from: BOOTSTRAP, AUTO (scheduled fetch), or MANUAL (admin). */
    @Column(name = "source", length = 16, nullable = false)
    private String source;

    /** When true, the scheduled updater will not overwrite the admin-set value. */
    @Column(name = "frozen", nullable = false)
    private boolean frozen;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
