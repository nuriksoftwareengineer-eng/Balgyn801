package com.nurba.java.component;

import com.nurba.java.domain.Design;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.DesignStatus;
import com.nurba.java.repositories.DesignGarmentRepository;
import com.nurba.java.repositories.DesignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralises design readiness logic so multiple services can call it
 * without circular-dependency issues.
 */
@Component
@RequiredArgsConstructor
public class DesignReadinessService {

    private final DesignRepository designRepository;
    private final DesignGarmentRepository garmentRepository;

    /**
     * Recomputes DRAFT ↔ READY based on current data.
     * PUBLISHED and ARCHIVED states are never touched automatically.
     */
    @Transactional
    public void recompute(Long designId) {
        Design design = designRepository.findById(designId).orElse(null);
        if (design == null) return;
        if (design.getStatus() == DesignStatus.PUBLISHED || design.getStatus() == DesignStatus.ARCHIVED) return;

        DesignStatus target = validationErrors(design).isEmpty() ? DesignStatus.READY : DesignStatus.DRAFT;
        if (design.getStatus() != target) {
            design.setStatus(target);
            designRepository.save(design);
        }
    }

    /**
     * Returns a list of human-readable reasons why the design cannot be published.
     * An empty list means the design is ready.
     */
    @Transactional(readOnly = true)
    public List<String> validationErrors(Design design) {
        List<String> errors = new ArrayList<>();

        if (design.getMainImageUrl() == null || design.getMainImageUrl().isBlank()) {
            errors.add("main_image_missing");
        }

        List<DesignGarment> active = garmentRepository.findByDesign_IdAndActiveTrue(design.getId());
        if (active.isEmpty()) {
            errors.add("no_active_garments");
        } else {
            // At least one active variant must have: KZT price + ≥1 color + ≥1 size
            boolean hasValidVariant = active.stream().anyMatch(g ->
                    g.getPrices().stream().anyMatch(p -> p.getCurrency() == Currency.KZT) &&
                    !g.getSizes().isEmpty() &&
                    !g.getColors().isEmpty()
            );
            if (!hasValidVariant) {
                errors.add("no_variant_with_kzt_price_size_color");
            }
        }

        return errors;
    }
}
