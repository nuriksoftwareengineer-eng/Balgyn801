package com.nurba.java.service;

import com.nurba.java.dto.responce.CatalogGroupDetailResponse;
import com.nurba.java.dto.responce.CatalogGroupResponse;
import com.nurba.java.dto.responce.CollectionDetailResponse;
import com.nurba.java.dto.responce.DesignDetailResponse;
import com.nurba.java.dto.responce.DesignResponse;

import java.util.List;

/**
 * Read-only service for public storefront catalog browsing.
 * Only returns active entities.
 */
public interface CatalogStorefrontService {

    /** All active catalog groups, sorted by sort_order. */
    List<CatalogGroupResponse> getGroups();

    /** Single group with its active collections. */
    CatalogGroupDetailResponse getGroupBySlug(String slug);

    /** Single collection with its active designs (no garment detail). */
    CollectionDetailResponse getCollectionBySlug(String slug);

    /**
     * Active designs.  If collectionId is provided, scoped to that collection;
     * otherwise returns all active designs across the catalog.
     */
    List<DesignResponse> getDesigns(Long collectionId);

    /**
     * Full design page: design info + active garments, each garment carrying
     * its prices (all currencies), available colors and available sizes.
     * Also increments the view count.
     */
    DesignDetailResponse getDesignBySlug(String slug);

    /** Popular designs sorted by view count, limited to {@code limit}. */
    List<DesignResponse> getPopular(int limit);

    /** New arrivals: designs with isNewArrival=true or published in last 30 days. */
    List<DesignResponse> getNewArrivals(int limit);

    /** Recommendations: same collection as the given design, fallback to popular. */
    List<DesignResponse> getRecommendations(Long designId, int limit);
}
