package com.nurba.java.service;

import com.nurba.java.dto.request.CreateDesignGarmentPriceRequest;
import com.nurba.java.dto.responce.DesignGarmentPriceResponse;

import java.util.List;

public interface DesignGarmentPriceService {
    List<DesignGarmentPriceResponse> getByGarment(Long designGarmentId);
    DesignGarmentPriceResponse upsert(CreateDesignGarmentPriceRequest request);
    void delete(Long id);
}
