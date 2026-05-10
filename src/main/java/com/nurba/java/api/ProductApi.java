package com.nurba.java.api;

import com.nurba.java.dto.request.CreateProductRequest;
import com.nurba.java.dto.responce.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Product", description = "Каталог товаров")
@RequestMapping("/api/v1/product")
public interface ProductApi {

    @Operation(summary = "Список товаров")
    @GetMapping
    List<ProductResponse> getAll();

    @Operation(summary = "Товар по ID")
    @GetMapping("/{id}")
    ProductResponse getById(@PathVariable Long id);

    @Operation(summary = "Создать товар")
    @PostMapping
    ProductResponse create(@RequestBody CreateProductRequest request);

    @Operation(summary = "Удалить товар")
    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
