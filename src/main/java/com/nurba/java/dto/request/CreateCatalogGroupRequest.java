package com.nurba.java.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCatalogGroupRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private Integer sortOrder = 0;

    private String coverImageUrl;

    private String bannerImageUrl;
}
