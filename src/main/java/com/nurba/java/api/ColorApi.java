package com.nurba.java.api;

import com.nurba.java.dto.request.CreateColorRequest;
import com.nurba.java.dto.responce.ColorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin / Colors")
@RequestMapping("/api/v1/admin/catalog/colors")
public interface ColorApi {

    @Operation(summary = "List all colors")
    @GetMapping
    List<ColorResponse> getAll();

    @Operation(summary = "Get color by ID")
    @GetMapping("/{id}")
    ColorResponse getById(@PathVariable Long id);

    @Operation(summary = "Create color")
    @PostMapping
    ColorResponse create(@Valid @RequestBody CreateColorRequest request);

    @Operation(summary = "Update color")
    @PutMapping("/{id}")
    ColorResponse update(@PathVariable Long id, @Valid @RequestBody CreateColorRequest request);

    @Operation(summary = "Delete color")
    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
