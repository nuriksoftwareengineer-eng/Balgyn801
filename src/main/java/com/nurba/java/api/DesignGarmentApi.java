package com.nurba.java.api;

import com.nurba.java.dto.request.CreateDesignGarmentRequest;
import com.nurba.java.dto.request.UpdateDesignGarmentRequest;
import com.nurba.java.dto.responce.DesignGarmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin / Design Garments")
@RequestMapping("/api/v1/admin/catalog/garments")
public interface DesignGarmentApi {

    @Operation(summary = "List garments by design")
    @GetMapping
    List<DesignGarmentResponse> getByDesign(@RequestParam Long designId);

    @Operation(summary = "Get garment by ID")
    @GetMapping("/{id}")
    DesignGarmentResponse getById(@PathVariable Long id);

    @Operation(summary = "Create garment variant for a design")
    @PostMapping
    DesignGarmentResponse create(@Valid @RequestBody CreateDesignGarmentRequest request);

    @Operation(summary = "Update garment variant (active flag, color set, size set)")
    @PutMapping("/{id}")
    DesignGarmentResponse update(@PathVariable Long id, @RequestBody UpdateDesignGarmentRequest request);

    @Operation(summary = "Delete garment variant")
    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
