package com.nurba.java.dto.responce;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nurba.java.enums.DesignStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DesignResponse {
    private Long id;
    private Long collectionId;
    private String collectionName;
    private String collectionNameKk;
    private String collectionNameEn;
    private String collectionSlug;
    private String groupName;
    private String groupNameKk;
    private String groupNameEn;
    private String groupSlug;
    private String name;
    private String nameKk;
    private String nameEn;
    private String slug;
    private String description;
    private String mainImageUrl;
    private List<String> gallery;
    private DesignStatus status;
    private Integer sortOrder;
    private LocalDateTime publishedAt;
    private LocalDateTime archivedAt;
    private Integer activeGarmentCount;
    @JsonProperty("isNewArrival")
    private boolean isNewArrival;
    private int viewCount;
}
