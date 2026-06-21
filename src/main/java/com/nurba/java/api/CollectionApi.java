package com.nurba.java.api;

import com.nurba.java.dto.request.CreateCollectionRequest;
import com.nurba.java.dto.responce.CollectionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin / Collections")
@RequestMapping("/api/v1/admin/catalog/collections")
public interface CollectionApi {

    @Operation(summary = "List collections (optionally filter by groupId)")
    @GetMapping
    List<CollectionResponse> getAll(@RequestParam(required = false) Long groupId);

    @Operation(summary = "Get collection by ID")
    @GetMapping("/{id}")
    CollectionResponse getById(@PathVariable Long id);

    @Operation(summary = "Create collection")
    @PostMapping
    CollectionResponse create(@Valid @RequestBody CreateCollectionRequest request);

    @Operation(summary = "Update collection")
    @PutMapping("/{id}")
    CollectionResponse update(@PathVariable Long id, @Valid @RequestBody CreateCollectionRequest request);

    @Operation(summary = "Delete collection")
    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
