package com.nurba.java.service;

import com.nurba.java.dto.request.CreateGarmentProfileRequest;
import com.nurba.java.dto.responce.GarmentProfileResponse;

import java.util.List;

/**
 * CRUD management of garment profiles (DB-driven garment type definitions).
 * Each profile stores the garment name, weight, and package dimensions (L×W×H)
 * used when creating CDEK shipments.
 */
public interface GarmentProfileService {

    List<GarmentProfileResponse> listAll();

    GarmentProfileResponse getById(Long id);

    GarmentProfileResponse create(CreateGarmentProfileRequest request);

    GarmentProfileResponse update(Long id, CreateGarmentProfileRequest request);

    void delete(Long id);
}
