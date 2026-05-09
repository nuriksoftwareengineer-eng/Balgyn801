package com.nurba.java.service;

import com.nurba.java.dto.request.CreateProductRequest;
import com.nurba.java.dto.responce.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse getById(Long id);
    List<ProductResponse> getAll();
    ProductResponse create(CreateProductRequest request);
    void delete(Long id);
}
