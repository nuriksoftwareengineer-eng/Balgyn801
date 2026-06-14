package com.nurba.java.dto.responce;

import lombok.Data;

@Data
public class DesignResponse {
    private Long id;
    private Long collectionId;
    private String collectionName;
    private String collectionSlug;
    private String groupName;
    private String groupSlug;
    private String name;
    private String slug;
    private String description;
    private String mainImageUrl;
    private Boolean active;
}
