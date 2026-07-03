package com.nurba.java.dto.responce;

import lombok.Data;

@Data
public class CatalogGroupResponse {
    private Long id;
    private String name;
    private String slug;
    private Integer sortOrder;
    private Boolean active;
    private String coverImageUrl;
    private String bannerImageUrl;
    private String nameKk;
    private String nameEn;
}
