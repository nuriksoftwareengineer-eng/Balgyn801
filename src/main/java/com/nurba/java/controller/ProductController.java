package com.nurba.java.controller;

import com.nurba.java.api.ProductApi;
import com.nurba.java.dto.request.CreateProductRequest;
import com.nurba.java.dto.responce.ProductResponse;
import com.nurba.java.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController implements ProductApi {

    private final ProductService productService;

    @Override
    public List<ProductResponse> getAll() {
        return productService.getAll();
    }

    @Override
    public ProductResponse getById(Long id) {
        return productService.getById(id);
    }

    @Override
    public ProductResponse create(CreateProductRequest request) {
        return productService.create(request);
    }

    @Override
    public void delete(Long id) {
        productService.delete(id);
    }
}
