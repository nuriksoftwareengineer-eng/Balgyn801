package com.nurba.java.dto.responce;

import com.nurba.java.enums.GarmentType;
import lombok.Data;

import java.util.List;

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
}
