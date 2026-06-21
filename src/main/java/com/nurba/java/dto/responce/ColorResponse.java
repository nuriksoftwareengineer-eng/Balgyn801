package com.nurba.java.dto.responce;

import lombok.Data;

@Data
public class ColorResponse {
    private Long id;
    private String name;
    private String hexCode;
    private Integer sortOrder;
}
