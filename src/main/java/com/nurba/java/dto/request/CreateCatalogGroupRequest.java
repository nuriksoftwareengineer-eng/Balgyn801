package com.nurba.java.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCatalogGroupRequest {

    @NotBlank
    private String name;

    /** Название на казахском (необязательное; публичный сайт падает обратно на name). */
    private String nameKk;

    /** Название на английском (необязательное; публичный сайт падает обратно на name). */
    private String nameEn;

    @NotBlank
    private String slug;

    private Integer sortOrder = 0;

    private String coverImageUrl;

    private String bannerImageUrl;
}
