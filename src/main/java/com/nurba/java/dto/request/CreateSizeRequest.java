package com.nurba.java.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSizeRequest {

    @NotBlank
    private String label;

    private Integer sortOrder = 0;
}
