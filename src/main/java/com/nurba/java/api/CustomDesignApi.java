package com.nurba.java.api;

import com.nurba.java.dto.request.CreateCustomDesignRequest;
import com.nurba.java.dto.responce.CustomDesignResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "Custom Design", description = "Кастомные дизайны")
@RequestMapping("/api/v1/custom-design")
public interface CustomDesignApi {

    @Operation(summary = "Список кастомных дизайнов")
    @GetMapping
    List<CustomDesignResponse> getAll();

    @Operation(summary = "Кастомный дизайн по ID")
    @GetMapping("/{id}")
    CustomDesignResponse getById(@PathVariable Long id);

    @Operation(summary = "Создать кастомный дизайн")
    @PostMapping
    CustomDesignResponse create(@RequestBody CreateCustomDesignRequest request);
}
