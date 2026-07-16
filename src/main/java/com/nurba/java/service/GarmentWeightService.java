package com.nurba.java.service;

import com.nurba.java.domain.OrderItem;
import com.nurba.java.dto.request.UpdateGarmentWeightRequest;
import com.nurba.java.dto.responce.GarmentWeightResponse;
import com.nurba.java.enums.GarmentType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Admin management and lookup of garment-type weights, plus order-weight calculation.
 * <p>
 * Weight is always resolved on the backend; the frontend never supplies it.
 */
public interface GarmentWeightService {

    /** Every garment type with its effective weight (DB row, or enum fallback if no row). */
    List<GarmentWeightResponse> listAll();

    /** Create or update the weight for a garment type (admin). */
    GarmentWeightResponse upsert(GarmentType garmentType, UpdateGarmentWeightRequest request);

    /** Effective weight (kg) for a single garment type: DB row if present, else the enum fallback. */
    BigDecimal weightForType(GarmentType garmentType);

    /** Total shippable weight (kg) of an order: SUM(garment weight × quantity) over design items. */
    BigDecimal calculateOrderWeight(List<OrderItem> items);

    /**
     * Same computation as {@link #calculateOrderWeight}, but from (designGarmentId → quantity)
     * pairs instead of persisted {@link OrderItem} rows — used for pre-checkout delivery quotes,
     * where no order exists yet. Unknown designGarmentId throws; missing/zero quantity is skipped.
     */
    BigDecimal calculateWeightForDesignGarments(Map<Long, Integer> quantityByDesignGarmentId);
}
