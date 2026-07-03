package com.nurba.java.dto.responce;

import lombok.Data;

import java.util.List;

/**
 * Storefront: one catalog group with its active collections embedded.
 */
@Data
public class CatalogGroupDetailResponse {
    private Long id;
    private String name;
    private String slug;
    private Integer sortOrder;
    private String coverImageUrl;
    private String bannerImageUrl;
    private String nameKk;
    private String nameEn;
    private List<CollectionResponse> collections;
}
