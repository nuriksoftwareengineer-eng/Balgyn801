package com.nurba.java.api;

import com.nurba.java.dto.request.CreateSizeRequest;
import com.nurba.java.dto.responce.SizeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin / Sizes")
@RequestMapping("/api/v1/admin/catalog/sizes")
public interface SizeApi {

    @Operation(summary = "List all sizes")
    @GetMapping
    List<SizeResponse> getAll();

    @Operation(summary = "Get size by ID")
    @GetMapping("/{id}")
    SizeResponse getById(@PathVariable Long id);

    @Operation(summary = "Create size")
    @PostMapping
    SizeResponse create(@Valid @RequestBody CreateSizeRequest request);

    @Operation(summary = "Update size")
    @PutMapping("/{id}")
    SizeResponse update(@PathVariable Long id, @Valid @RequestBody CreateSizeRequest request);

    @Operation(summary = "Delete size")
    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
