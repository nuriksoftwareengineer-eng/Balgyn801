package com.nurba.java.api;

import com.nurba.java.dto.request.CreateCatalogGroupRequest;
import com.nurba.java.dto.responce.CatalogGroupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin / Catalog Groups")
@RequestMapping("/api/v1/admin/catalog/groups")
public interface CatalogGroupApi {

    @Operation(summary = "List all catalog groups")
    @GetMapping
    List<CatalogGroupResponse> getAll();

    @Operation(summary = "Get catalog group by ID")
    @GetMapping("/{id}")
    CatalogGroupResponse getById(@PathVariable Long id);

    @Operation(summary = "Create catalog group")
    @PostMapping
    CatalogGroupResponse create(@Valid @RequestBody CreateCatalogGroupRequest request);

    @Operation(summary = "Update catalog group")
    @PutMapping("/{id}")
    CatalogGroupResponse update(@PathVariable Long id, @Valid @RequestBody CreateCatalogGroupRequest request);

    @Operation(summary = "Delete catalog group")
    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
