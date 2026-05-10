package com.nurba.java.dto.request;

import com.nurba.java.model.ProductColorOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {
    private String title;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Boolean inStock;
    private String category;
    private List<String> sizes;
    private List<ProductColorOption> colors;
}
