package com.nurba.java.mapper;

import com.nurba.java.domain.Product;
import com.nurba.java.dto.request.CreateProductRequest;
import com.nurba.java.dto.responce.ProductResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    Product toEntity(CreateProductRequest request);

    ProductResponse toResponse(Product product);
}