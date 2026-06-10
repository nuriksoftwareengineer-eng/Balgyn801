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

    private Integer sortOrder = 0;
}
