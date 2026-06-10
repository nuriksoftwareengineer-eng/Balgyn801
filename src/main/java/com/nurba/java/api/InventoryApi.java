package com.nurba.java.api;

import com.nurba.java.dto.request.SetInventoryRequest;
import com.nurba.java.dto.responce.InventoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin / Inventory")
@RequestMapping("/api/v1/admin/catalog/inventory")
public interface InventoryApi {

    @Operation(summary = "List inventory for a garment variant")
    @GetMapping
    List<InventoryResponse> getByGarment(@RequestParam Long designGarmentId);

    @Operation(summary = "Set inventory quantity (create or update)")
    @PostMapping
    InventoryResponse set(@Valid @RequestBody SetInventoryRequest request);

    @Operation(summary = "Delete an inventory record by ID")
    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);
}
