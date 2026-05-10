package com.nurba.java.service.Impl;

import com.nurba.java.constants.StoreCategories;
import com.nurba.java.domain.Product;
import com.nurba.java.dto.request.CreateProductRequest;
import com.nurba.java.dto.responce.ProductResponse;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.ProductMapper;
import com.nurba.java.repositories.ProductRepository;
import com.nurba.java.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Товар не найден"));
        return productMapper.toResponse(product);
    }

    @Override
    public List<ProductResponse> getAll(String category) {
        List<Product> list;
        if (StoreCategories.isNoFilter(category)) {
            list = productRepository.findAll();
        } else {
            list = productRepository.findByCategory(category.trim());
        }
        return list.stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public ProductResponse create(CreateProductRequest request) {
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new BusinessRuleException("Категория обязательна — выберите такую же, как на главной витрине");
        }
        if (!StoreCategories.isProductCategory(request.getCategory())) {
            throw new BusinessRuleException("Неизвестная категория. Допустимы: " + StoreCategories.PRODUCT_CATEGORY_LABELS);
        }
        Product product = productMapper.toEntity(request);
        product.setCategory(request.getCategory().trim());
        if (product.getSizes() == null) {
            product.setSizes(new ArrayList<>());
        }
        if (product.getColors() == null) {
            product.setColors(new ArrayList<>());
        }
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
