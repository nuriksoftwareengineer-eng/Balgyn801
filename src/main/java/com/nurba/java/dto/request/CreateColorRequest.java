package com.nurba.java.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateColorRequest {

    @NotBlank
    private String name;

    private String hexCode;

    private Integer sortOrder = 0;
}
