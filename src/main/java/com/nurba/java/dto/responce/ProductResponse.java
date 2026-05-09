package com.nurba.java.dto.responce;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Boolean inStock;
}
