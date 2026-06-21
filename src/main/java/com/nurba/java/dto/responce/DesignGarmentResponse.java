package com.nurba.java.dto.responce;

import com.nurba.java.enums.GarmentType;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DesignGarmentResponse {
    private Long id;
    private Long designId;
    private String designName;
    private GarmentType garmentType;
    private Boolean active;
    private List<DesignGarmentPriceResponse> prices;
    private List<ColorResponse> colors;
    private List<SizeResponse> sizes;
    /** colorId → sizeId → quantity. Only populated on the storefront detail endpoint. */
    private Map<Long, Map<Long, Integer>> stockMap;
}
