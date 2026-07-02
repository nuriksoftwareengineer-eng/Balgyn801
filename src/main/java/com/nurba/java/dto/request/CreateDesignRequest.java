package com.nurba.java.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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

    /** Дополнительные изображения галереи (список URL). Главное фото — в mainImageUrl. */
    private List<String> gallery = new ArrayList<>();

    @JsonProperty("isNewArrival")
    private boolean isNewArrival = false;
}
