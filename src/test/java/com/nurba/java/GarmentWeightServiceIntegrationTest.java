package com.nurba.java;

import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.GarmentProfile;
import com.nurba.java.domain.OrderItem;
import com.nurba.java.dto.request.UpdateGarmentWeightRequest;
import com.nurba.java.dto.responce.GarmentWeightResponse;
import com.nurba.java.enums.GarmentType;
import com.nurba.java.repositories.GarmentTypeWeightRepository;
import com.nurba.java.service.GarmentWeightService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 1 — verifies garment weights are DB-authoritative with an enum fallback,
 * and that order weight is computed backend-side from design items.
 */
@SpringBootTest
@ActiveProfiles("test")
class GarmentWeightServiceIntegrationTest {

    @Autowired private GarmentWeightService service;
    @Autowired private GarmentTypeWeightRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // Don't leak garment weights into the shared in-memory DB used by other test classes.
        repository.deleteAll();
    }

    @Test
    void weightForType_fallsBackToEnumDefault_whenNoRow() {
        // No DB row → enum fallback applies (HOODIE = 1.000 kg).
        assertThat(service.weightForType(GarmentType.HOODIE))
                .isEqualByComparingTo(GarmentType.HOODIE.getDefaultWeightKg());
    }

    @Test
    void upsert_thenWeightForType_returnsDbValue() {
        service.upsert(GarmentType.HOODIE, new UpdateGarmentWeightRequest(new BigDecimal("1.250")));
        // DB value overrides the enum fallback.
        assertThat(service.weightForType(GarmentType.HOODIE)).isEqualByComparingTo("1.250");
    }

    @Test
    void listAll_returnsEveryGarmentType() {
        List<GarmentWeightResponse> all = service.listAll();
        assertThat(all).hasSize(GarmentType.values().length);
        assertThat(all).extracting(GarmentWeightResponse::garmentType)
                .containsExactlyInAnyOrder(GarmentType.values());
    }

    @Test
    void calculateOrderWeight_sumsDesignItemsOnly() {
        OrderItem hoodie = designItem(new BigDecimal("1.000"), 2);    // 2 × 1.000 = 2.000
        OrderItem tshirt = designItem(new BigDecimal("0.400"), 3);   // 3 × 0.400 = 1.200
        OrderItem legacyProduct = new OrderItem();               // no garment → no weight
        legacyProduct.setQuantity(5);

        BigDecimal total = service.calculateOrderWeight(List.of(hoodie, tshirt, legacyProduct));
        assertThat(total).isEqualByComparingTo("3.200");
    }

    private OrderItem designItem(BigDecimal weightKg, int qty) {
        GarmentProfile profile = new GarmentProfile();
        profile.setName("test");
        profile.setWeightKg(weightKg);
        profile.setLengthCm(30);
        profile.setWidthCm(25);
        profile.setHeightCm(5);
        profile.setSortOrder(0);
        DesignGarment garment = new DesignGarment();
        garment.setGarmentProfile(profile);
        OrderItem item = new OrderItem();
        item.setDesignGarment(garment);
        item.setQuantity(qty);
        return item;
    }
}
