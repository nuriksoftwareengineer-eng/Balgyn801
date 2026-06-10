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
    private String name;
    private String slug;
    private List<DesignResponse> designs;
}
