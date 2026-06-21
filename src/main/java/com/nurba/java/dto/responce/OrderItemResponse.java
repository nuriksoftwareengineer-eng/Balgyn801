package com.nurba.java.dto.responce;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private Long id;

    // ── Product-based fields (null for design-based orders) ────────────────
    private String productTitle;

    // ── Design-based fields (null for product-based orders) ────────────────
    private Long   designGarmentId;
    private String garmentType;
    private String designName;
    private Long   colorId;
    private String colorHex;
    private Long   sizeId;

    // ── Shared ─────────────────────────────────────────────────────────────
    private Integer    quantity;
    private BigDecimal unitPrice;
    /**
     * Human-readable size label.
     * Product-based: from storefront string. Design-based: from Size.label.
     */
    private String sizeLabel;
    /**
     * Human-readable color name.
     * Product-based: from storefront string. Design-based: from Color.name.
     */
    private String  colorName;
    /** Currency of unitPrice (null for product-based orders). */
    private String  currency;
}
