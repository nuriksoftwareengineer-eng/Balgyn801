package com.nurba.java.dto.responce;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private Long id;
    private String productTitle;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String sizeLabel;
    private String colorName;
}
