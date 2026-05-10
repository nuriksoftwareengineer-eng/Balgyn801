package com.nurba.java.service;

import com.nurba.java.dto.request.CreateCustomDesignRequest;
import com.nurba.java.dto.responce.CustomDesignResponse;

import java.util.List;

public interface CustomDesignService {

    CustomDesignResponse create(CreateCustomDesignRequest request);
    CustomDesignResponse getById(Long id);
    List<CustomDesignResponse> getAll();
}
