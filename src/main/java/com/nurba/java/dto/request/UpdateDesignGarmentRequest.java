package com.nurba.java.dto.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateDesignGarmentRequest {

    /** Set to true/false to enable or disable this garment variant. */
    private Boolean active;

    /** Full replacement of the available color set. Pass empty list to clear. */
    private List<Long> colorIds = new ArrayList<>();

    /** Full replacement of the available size set. Pass empty list to clear. */
    private List<Long> sizeIds = new ArrayList<>();
}
