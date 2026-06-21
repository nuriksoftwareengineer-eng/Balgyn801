package com.nurba.java.service;

import com.nurba.java.dto.request.CreateSizeRequest;
import com.nurba.java.dto.responce.SizeResponse;

import java.util.List;

public interface SizeService {
    List<SizeResponse> getAll();
    SizeResponse getById(Long id);
    SizeResponse create(CreateSizeRequest request);
    SizeResponse update(Long id, CreateSizeRequest request);
    void delete(Long id);
}
