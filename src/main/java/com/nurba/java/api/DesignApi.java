package com.nurba.java.api;

import com.nurba.java.dto.request.CreateDesignRequest;
import com.nurba.java.dto.responce.DesignResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin / Designs")
@RequestMapping("/api/v1/admin/catalog/designs")
public interface DesignApi {

    @Operation(summary = "List designs (optionally filter by collectionId)")
    @GetMapping
    List<DesignResponse> getAll(@RequestParam(required = false) Long collectionId);

    @Operation(summary = "Get design by ID")
    @GetMapping("/{id}")
    DesignResponse getById(@PathVariable Long id);

    @Operation(summary = "Create design")
    @PostMapping
    DesignResponse create(@Valid @RequestBody CreateDesignRequest request);

    @Operation(summary = "Update design")
    @PutMapping("/{id}")
    DesignResponse update(@PathVariable Long id, @Valid @RequestBody CreateDesignRequest request);

    @Operation(summary = "Delete design")
    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
