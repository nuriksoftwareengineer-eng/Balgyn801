package com.nurba.java.service;

import com.nurba.java.dto.request.CreateDesignRequest;
import com.nurba.java.dto.responce.DesignResponse;

import java.util.List;

public interface DesignService {
    List<DesignResponse> getAll(Long collectionId);
    DesignResponse getById(Long id);
    DesignResponse create(CreateDesignRequest request);
    DesignResponse update(Long id, CreateDesignRequest request);
    void delete(Long id);
}
