package com.nurba.java.dto.responce;

import lombok.Data;

import java.util.List;

/**
 * Storefront: full design detail — the page where the customer chooses
 * garment type, color, size and currency before adding to cart.
 */
@Data
public class DesignDetailResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String mainImageUrl;
    private Long collectionId;
    private String collectionName;
    private String collectionSlug;
    private String groupName;
    private String groupSlug;

    /** Active garment variants; each carries its own prices / colors / sizes. */
    private List<DesignGarmentResponse> garments;
}
