package com.nurba.java.dto.responce;

import com.nurba.java.enums.CustomDesignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomDesignResponse {
    private Long id;
    private Long customerId;
    private String description;
    private String referenceImageUrl;
    private CustomDesignStatus status;
    private LocalDateTime createdAt;
}
