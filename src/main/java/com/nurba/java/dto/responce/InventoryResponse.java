package com.nurba.java.dto.responce;

import lombok.Data;

@Data
public class InventoryResponse {
    private Long id;
    private Long designGarmentId;
    private Long colorId;
    private String colorName;
    private Long sizeId;
    private String sizeLabel;
    private Integer quantity;
}
