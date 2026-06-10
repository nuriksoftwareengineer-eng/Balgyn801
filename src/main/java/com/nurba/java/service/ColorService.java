package com.nurba.java.service;

import com.nurba.java.dto.request.CreateColorRequest;
import com.nurba.java.dto.responce.ColorResponse;

import java.util.List;

public interface ColorService {
    List<ColorResponse> getAll();
    ColorResponse getById(Long id);
    ColorResponse create(CreateColorRequest request);
    ColorResponse update(Long id, CreateColorRequest request);
    void delete(Long id);
}
