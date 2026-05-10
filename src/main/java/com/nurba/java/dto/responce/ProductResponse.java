package com.nurba.java.dto.responce;

import com.nurba.java.model.ProductColorOption;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

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
    private String category;
    private List<String> sizes;
    private List<ProductColorOption> colors;
}
