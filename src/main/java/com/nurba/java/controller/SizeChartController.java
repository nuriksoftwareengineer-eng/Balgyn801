package com.nurba.java.controller;

import com.nurba.java.domain.SizeChartImage;
import com.nurba.java.dto.request.UpsertSizeChartRequest;
import com.nurba.java.dto.responce.SizeChartImageResponse;
import com.nurba.java.enums.GarmentType;
import com.nurba.java.repositories.SizeChartImageRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SizeChartController {

    private final SizeChartImageRepository repo;

    // ── Public ──────────────────────────────────────────────────────────────

    /** All active size chart images — used by the storefront to show size guide buttons. */
    @GetMapping("/catalog/size-charts")
    public List<SizeChartImageResponse> getAll() {
        return repo.findAllByActiveTrueOrderByGarmentTypeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Admin ────────────────────────────────────────────────────────────────

    /** Upsert: creates or replaces the size chart for a given garment type. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/size-charts")
    public SizeChartImageResponse upsert(@Valid @RequestBody UpsertSizeChartRequest req) {
        SizeChartImage img = repo.findByGarmentType(req.getGarmentType())
                .orElseGet(SizeChartImage::new);
        img.setGarmentType(req.getGarmentType());
        img.setImageUrl(req.getImageUrl());
        img.setTitle(req.getTitle());
        img.setActive(true);
        img.setUpdatedAt(LocalDateTime.now());
        return toResponse(repo.save(img));
    }

    /** Soft-delete: sets active=false so the button disappears from the storefront. */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/size-charts/{garmentType}")
    public ResponseEntity<Void> delete(@PathVariable GarmentType garmentType) {
        repo.findByGarmentType(garmentType).ifPresent(img -> {
            img.setActive(false);
            img.setUpdatedAt(LocalDateTime.now());
            repo.save(img);
        });
        return ResponseEntity.noContent().build();
    }

    private SizeChartImageResponse toResponse(SizeChartImage img) {
        SizeChartImageResponse r = new SizeChartImageResponse();
        r.setId(img.getId());
        r.setGarmentType(img.getGarmentType().name());
        r.setImageUrl(img.getImageUrl());
        r.setTitle(img.getTitle());
        return r;
    }
}
