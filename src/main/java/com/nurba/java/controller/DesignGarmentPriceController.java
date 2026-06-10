package com.nurba.java.controller;

import com.nurba.java.api.DesignGarmentPriceApi;
import com.nurba.java.dto.request.CreateDesignGarmentPriceRequest;
import com.nurba.java.dto.responce.DesignGarmentPriceResponse;
import com.nurba.java.service.DesignGarmentPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DesignGarmentPriceController implements DesignGarmentPriceApi {

    private final DesignGarmentPriceService service;

    @Override
    public List<DesignGarmentPriceResponse> getByGarment(Long designGarmentId) {
        return service.getByGarment(designGarmentId);
    }

    @Override
    public DesignGarmentPriceResponse upsert(CreateDesignGarmentPriceRequest request) {
        return service.upsert(request);
    }

    @Override
    public void delete(Long id) {
        service.delete(id);
    }
}
