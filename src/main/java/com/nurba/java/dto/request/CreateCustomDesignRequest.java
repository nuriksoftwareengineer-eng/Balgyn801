package com.nurba.java.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCustomDesignRequest {
    private Long customerId;
    @Size(max = 5000)
    private String description;
    @Size(max = 500)
    @Pattern(regexp = "^(https?)://[\\w.-].*", message = "referenceImageUrl должен быть валидным http/https URL")
    private String referenceImageUrl;
}
