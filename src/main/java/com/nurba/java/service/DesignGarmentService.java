package com.nurba.java.service;

import com.nurba.java.dto.request.CreateDesignGarmentRequest;
import com.nurba.java.dto.request.UpdateDesignGarmentRequest;
import com.nurba.java.dto.responce.DesignGarmentResponse;

import java.util.List;

public interface DesignGarmentService {
    List<DesignGarmentResponse> getByDesign(Long designId);
    DesignGarmentResponse getById(Long id);
    DesignGarmentResponse create(CreateDesignGarmentRequest request);
    DesignGarmentResponse update(Long id, UpdateDesignGarmentRequest request);
    void delete(Long id);
}
