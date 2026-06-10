package com.nurba.java.api;

import com.nurba.java.dto.request.CreateDesignGarmentPriceRequest;
import com.nurba.java.dto.responce.DesignGarmentPriceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin / Design Garment Prices")
@RequestMapping("/api/v1/admin/catalog/prices")
public interface DesignGarmentPriceApi {

    @Operation(summary = "List prices for a garment variant")
    @GetMapping
    List<DesignGarmentPriceResponse> getByGarment(@RequestParam Long designGarmentId);

    @Operation(summary = "Create or update price for a currency (upsert)")
    @PostMapping
    DesignGarmentPriceResponse upsert(@Valid @RequestBody CreateDesignGarmentPriceRequest request);

    @Operation(summary = "Delete price by ID")
    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
