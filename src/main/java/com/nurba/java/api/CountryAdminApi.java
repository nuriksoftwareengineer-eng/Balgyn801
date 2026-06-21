package com.nurba.java.api;

import com.nurba.java.dto.request.UpsertCountryRequest;
import com.nurba.java.dto.responce.AdminCountryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin / Countries")
@RequestMapping("/api/v1/admin/catalog/countries")
public interface CountryAdminApi {

    @Operation(summary = "List all countries (with zone + active flag)")
    @GetMapping
    List<AdminCountryResponse> listAll();

    @Operation(summary = "Create a country")
    @PostMapping
    AdminCountryResponse create(@Valid @RequestBody UpsertCountryRequest request);

    @Operation(summary = "Update a country")
    @PutMapping("/{id}")
    AdminCountryResponse update(@PathVariable Long id, @Valid @RequestBody UpsertCountryRequest request);

    @Operation(summary = "Delete a country")
    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
