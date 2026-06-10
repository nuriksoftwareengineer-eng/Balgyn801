package com.nurba.java.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {

    // ── Product-based order (existing, untouched) ──────────────────────────
    private Long productId;
    private Long customDesignId;
    /** Variant size string for product-based orders. */
    private String size;
    /** Variant color string for product-based orders. */
    private String color;

    // ── Design-based order (new catalog path) ──────────────────────────────
    /** DesignGarment ID from the catalog. Mutually exclusive with productId. */
    private Long designGarmentId;
    /** Color entity ID chosen on the design page. */
    private Long colorId;
    /** Size entity ID chosen on the design page. */
    private Long sizeId;
    /**
     * Currency code for the design price (e.g. "KZT", "RUB", "USD").
     * Defaults to "KZT" when null.
     */
    private String currency;

    // ── Shared ─────────────────────────────────────────────────────────────
    private Integer quantity;
}
