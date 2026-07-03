package com.nurba.java.api;

import com.nurba.java.dto.request.CreateGarmentProfileRequest;
import com.nurba.java.dto.responce.GarmentProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin / Garment profiles")
@RequestMapping("/api/v1/admin/catalog/garment-profiles")
public interface GarmentProfileApi {

    @Operation(summary = "List all garment profiles (sorted by sort_order, name)")
    @GetMapping
    List<GarmentProfileResponse> listAll();

    @Operation(summary = "Get one garment profile by id")
    @GetMapping("/{id}")
    GarmentProfileResponse getById(@PathVariable Long id);

    @Operation(summary = "Create a new garment profile")
    @PostMapping
    GarmentProfileResponse create(@Valid @RequestBody CreateGarmentProfileRequest request);

    @Operation(summary = "Update a garment profile")
    @PutMapping("/{id}")
    GarmentProfileResponse update(@PathVariable Long id,
                                  @Valid @RequestBody CreateGarmentProfileRequest request);

    @Operation(summary = "Delete a garment profile (fails if used by any design variant)")
    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
