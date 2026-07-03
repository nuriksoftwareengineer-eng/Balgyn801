package com.nurba.java.service.Impl;

import com.nurba.java.domain.GarmentTypeWeight;
import com.nurba.java.domain.OrderItem;
import com.nurba.java.dto.request.UpdateGarmentWeightRequest;
import com.nurba.java.dto.responce.GarmentWeightResponse;
import com.nurba.java.enums.GarmentType;
import com.nurba.java.repositories.GarmentTypeWeightRepository;
import com.nurba.java.service.GarmentWeightService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GarmentWeightServiceImpl implements GarmentWeightService {

    private static final Logger log = LoggerFactory.getLogger(GarmentWeightServiceImpl.class);
    private static final int WEIGHT_SCALE = 3;

    private final GarmentTypeWeightRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<GarmentWeightResponse> listAll() {
        // Always return every garment type, falling back to the enum default for any missing row,
        // so the admin panel shows a complete, editable list even on a freshly-seeded database.
        return Arrays.stream(GarmentType.values())
                .map(type -> new GarmentWeightResponse(type, weightForType(type)))
                .toList();
    }

    @Override
    @Transactional
    public GarmentWeightResponse upsert(GarmentType garmentType, UpdateGarmentWeightRequest request) {
        GarmentTypeWeight entity = repository.findById(garmentType)
                .orElseGet(GarmentTypeWeight::new);
        entity.setGarmentType(garmentType);
        entity.setWeightKg(request.weightKg().setScale(WEIGHT_SCALE, RoundingMode.HALF_UP));
        GarmentTypeWeight saved = repository.save(entity);
        return new GarmentWeightResponse(saved.getGarmentType(), saved.getWeightKg());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal weightForType(GarmentType garmentType) {
        return repository.findById(garmentType)
                .map(GarmentTypeWeight::getWeightKg)
                .orElseGet(() -> {
                    log.warn("No garment_type_weights row for {} — using enum fallback {} kg",
                            garmentType, garmentType.getDefaultWeightKg());
                    return garmentType.getDefaultWeightKg();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateOrderWeight(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            // Only design-based items carry a garment profile (and therefore a weight).
            // Legacy product-based items contribute no shippable weight in this design-first store.
            if (item.getDesignGarment() == null || item.getDesignGarment().getGarmentProfile() == null) {
                continue;
            }
            int qty = item.getQuantity() == null ? 0 : item.getQuantity();
            if (qty <= 0) {
                continue;
            }
            BigDecimal unit = item.getDesignGarment().getGarmentProfile().getWeightKg();
            total = total.add(unit.multiply(BigDecimal.valueOf(qty)));
        }
        return total.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
    }
}
