package com.nurba.java.dto.responce;

import lombok.Data;

import java.util.List;

/**
 * Storefront: one collection with its active designs embedded.
 */
@Data
public class CollectionDetailResponse {
    private Long id;
    private Long groupId;
    private String groupName;
    private String groupNameKk;
    private String groupNameEn;
    private String name;
    private String nameKk;
    private String nameEn;
    private String slug;
    private String description;
    private String coverImageUrl;
    private String bannerImageUrl;
    private List<DesignResponse> designs;
}
