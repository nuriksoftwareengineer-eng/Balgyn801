package com.nurba.java.api;

import com.nurba.java.dto.request.UpdateGarmentWeightRequest;
import com.nurba.java.dto.responce.GarmentWeightResponse;
import com.nurba.java.enums.GarmentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin / Garment weights")
@RequestMapping("/api/v1/admin/catalog/garment-weights")
public interface GarmentWeightApi {

    @Operation(summary = "List all garment-type weights (kg)")
    @GetMapping
    List<GarmentWeightResponse> listAll();

    @Operation(summary = "Set the weight (kg) of a garment type")
    @PutMapping("/{garmentType}")
    GarmentWeightResponse upsert(
            @PathVariable GarmentType garmentType,
            @Valid @RequestBody UpdateGarmentWeightRequest request
    );
}
