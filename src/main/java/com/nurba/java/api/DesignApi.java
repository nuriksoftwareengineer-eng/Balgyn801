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

    @Operation(summary = "Create design — starts as DRAFT")
    @PostMapping
    DesignResponse create(@Valid @RequestBody CreateDesignRequest request);

    @Operation(summary = "Update design metadata")
    @PutMapping("/{id}")
    DesignResponse update(@PathVariable Long id, @Valid @RequestBody CreateDesignRequest request);

    @Operation(summary = "Duplicate a design — copies garments/colors/sizes/prices into a new " +
            "DRAFT design; inventory, status, view count and publish/archive dates are reset")
    @PostMapping("/{id}/duplicate")
    DesignResponse duplicate(@PathVariable Long id);

    @Operation(summary = "Publish a design (DRAFT/READY → PUBLISHED). Returns 400 if requirements not met.")
    @PatchMapping("/{id}/publish")
    DesignResponse publish(@PathVariable Long id);

    @Operation(summary = "Unpublish a design (PUBLISHED → READY or DRAFT). Removes from public catalog.")
    @PatchMapping("/{id}/unpublish")
    DesignResponse unpublish(@PathVariable Long id);

    @Operation(summary = "Archive a design (PUBLISHED → ARCHIVED). Sets archivedAt timestamp.")
    @PatchMapping("/{id}/archive")
    DesignResponse archive(@PathVariable Long id);

    @Operation(summary = "Restore an archived design (ARCHIVED → DRAFT). Clears archivedAt.")
    @PatchMapping("/{id}/restore")
    DesignResponse restore(@PathVariable Long id);

    @Operation(summary = "Delete design")
    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
