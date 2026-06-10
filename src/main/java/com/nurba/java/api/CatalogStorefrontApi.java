package com.nurba.java.api;

import com.nurba.java.dto.responce.CatalogGroupDetailResponse;
import com.nurba.java.dto.responce.CatalogGroupResponse;
import com.nurba.java.dto.responce.CollectionDetailResponse;
import com.nurba.java.dto.responce.DesignDetailResponse;
import com.nurba.java.dto.responce.DesignResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Catalog (public)", description = "Public storefront catalog — no authentication required")
@RequestMapping("/api/v1/catalog")
public interface CatalogStorefrontApi {

    @Operation(summary = "All active catalog groups")
    @GetMapping("/groups")
    List<CatalogGroupResponse> getGroups();

    @Operation(summary = "Group detail: group info + its active collections")
    @GetMapping("/groups/{slug}")
    CatalogGroupDetailResponse getGroupBySlug(@PathVariable String slug);

    @Operation(summary = "Collection detail: collection info + its active designs")
    @GetMapping("/collections/{slug}")
    CollectionDetailResponse getCollectionBySlug(@PathVariable String slug);

    @Operation(summary = "Active designs — optionally scoped to a collection")
    @GetMapping("/designs")
    List<DesignResponse> getDesigns(@RequestParam(required = false) Long collectionId);

    @Operation(summary = "Full design page: garments, prices (all currencies), colors, sizes")
    @GetMapping("/designs/{slug}")
    DesignDetailResponse getDesignBySlug(@PathVariable String slug);
}
