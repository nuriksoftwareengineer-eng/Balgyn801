package com.nurba.java.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCollectionRequest {

    @NotNull
    private Long groupId;

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String description;

    /** URL обложки коллекции (через POST /api/v1/media/upload). */
    private String coverImageUrl;

    /** URL баннера коллекции (через POST /api/v1/media/upload). */
    private String bannerImageUrl;

    private Integer sortOrder = 0;
}
