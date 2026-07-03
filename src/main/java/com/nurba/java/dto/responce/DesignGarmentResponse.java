package com.nurba.java.dto.responce;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DesignGarmentResponse {
    private Long id;
    private Long designId;
    private String designName;
    /** Profile id — used by admin UI to know which profile is selected. */
    private Long garmentProfileId;
    /** Profile name (English) — used as fallback display label. */
    private String garmentType;
    /** Russian localized garment label from the profile. */
    private String garmentTypeRu;
    /** Kazakh localized garment label from the profile. */
    private String garmentTypeKk;
    private Boolean active;
    private List<DesignGarmentPriceResponse> prices;
    private List<ColorResponse> colors;
    private List<SizeResponse> sizes;
    /** colorId → sizeId → quantity. Only populated on the storefront detail endpoint. */
    private Map<Long, Map<Long, Integer>> stockMap;
}
