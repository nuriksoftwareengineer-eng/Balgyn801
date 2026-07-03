package com.nurba.java.dto.responce;

import lombok.Data;

@Data
public class CollectionResponse {
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
    private Integer sortOrder;
    private Boolean active;
}
