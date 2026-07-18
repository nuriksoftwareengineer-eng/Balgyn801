package com.nurba.java.service;

import com.nurba.java.dto.request.CreateDesignRequest;
import com.nurba.java.dto.responce.DesignResponse;

import java.util.List;

public interface DesignService {
    List<DesignResponse> getAll(Long collectionId);
    DesignResponse getById(Long id);
    DesignResponse create(CreateDesignRequest request);
    DesignResponse update(Long id, CreateDesignRequest request);
    void delete(Long id);

    /** Copies a design's own fields plus its garments/colors/sizes/prices into a brand-new
     *  DRAFT design. Inventory is intentionally not copied — the copy starts unstocked. */
    DesignResponse duplicate(Long id);

    /** Validates requirements and transitions DRAFT/READY → PUBLISHED. Sets publishedAt on first publish. */
    DesignResponse publish(Long id);

    /** PUBLISHED → READY (if requirements met) or DRAFT. Clears published state from storefront. */
    DesignResponse unpublish(Long id);

    /** Transitions PUBLISHED → ARCHIVED. Sets archivedAt. */
    DesignResponse archive(Long id);

    /** Transitions ARCHIVED → DRAFT. Clears archivedAt. */
    DesignResponse restore(Long id);

    /** Recomputes DRAFT ↔ READY for a design after external changes (e.g. garment update). */
    void recomputeStatus(Long designId);
}
