package com.nurba.java.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDesignRequest {

    @NotNull
    private Long collectionId;

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String description;

    private String mainImageUrl;
}
