package com.nurba.java;

import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Design;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.DesignGarmentPrice;
import com.nurba.java.domain.Product;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.GarmentType;
import com.nurba.java.repositories.CatalogGroupRepository;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.DesignGarmentPriceRepository;
import com.nurba.java.repositories.DesignGarmentRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.repositories.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for POST /api/v1/delivery/cdek/calculate-order.
 *
 * Verifies that the endpoint handles both legacy product items (heuristic weight)
 * and design-garment items (actual garment-type weight via GarmentWeightService),
 * as well as mixed carts and error cases.
 *
 * CDEK client is in stub mode (no API credentials in test profile); delivery fees
 * are therefore deterministic:  fee = 1500 + 400 × ceil(weightGrams / 1000, 2 dp).
 */
@SpringBootTest
@ActiveProfiles("test")
class CdekCalculateOrderIntegrationTest {

    private static final int CDEK_STUB_CITY = 270;  // Алматы — exists in stub cities list

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    // ── Repositories ─────────────────────────────────────────────────────────
    @Autowired private ProductRepository productRepository;
    @Autowired private CatalogGroupRepository catalogGroupRepository;
    @Autowired private CollectionRepository collectionRepository;
    @Autowired private DesignRepository designRepository;
    @Autowired private DesignGarmentRepository designGarmentRepository;
    @Autowired private DesignGarmentPriceRepository designGarmentPriceRepository;

    // ── Fixture IDs ──────────────────────────────────────────────────────────
    private Long productId;          // legacy product, price 5 000 KZT
    private Long hoodieGarmentId;    // HOODIE (1.000 kg default), price 12 000 KZT
    private Long noKztGarmentId;     // T_SHIRT, NO KZT price row (triggers 400)

    // ─────────────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        cleanAll();
        buildFixture();
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
        designGarmentPriceRepository.deleteAll();
        designGarmentRepository.deleteAll();
        designRepository.deleteAll();
        collectionRepository.deleteAll();
        catalogGroupRepository.deleteAll();
        productRepository.deleteAll();
    }

    private void buildFixture() {
        // ── Legacy product ────────────────────────────────────────────────────
        Product product = new Product();
        product.setTitle("Cdek Test Product");
        product.setPrice(new BigDecimal("5000.00"));
        product.setInStock(true);
        productId = productRepository.save(product).getId();

        // ── Catalog skeleton (required by FK chain: Group → Collection → Design) ──
        CatalogGroup group = new CatalogGroup();
        group.setName("Cdek Test Group");
        group.setSlug("cdek-test-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("Cdek Test Collection");
        coll.setSlug("cdek-test-collection");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("Cdek Test Design");
        design.setSlug("cdek-test-design");
        design.setStatus(com.nurba.java.enums.DesignStatus.PUBLISHED);
        design.setCreatedAt(LocalDateTime.now());
        design = designRepository.save(design);

        // ── HOODIE garment with KZT price ─────────────────────────────────────
        DesignGarment hoodie = new DesignGarment();
        hoodie.setDesign(design);
        hoodie.setGarmentType(GarmentType.HOODIE);   // default weight: 1.000 kg = 1000 g
        hoodie.setActive(true);
        hoodie = designGarmentRepository.save(hoodie);
        hoodieGarmentId = hoodie.getId();

        DesignGarmentPrice hoodieKztPrice = new DesignGarmentPrice();
        hoodieKztPrice.setDesignGarment(hoodie);
        hoodieKztPrice.setCurrency(Currency.KZT);
        hoodieKztPrice.setAmount(new BigDecimal("12000.00"));
        designGarmentPriceRepository.save(hoodieKztPrice);

        // ── T_SHIRT garment with NO KZT price (for error-case test) ──────────
        DesignGarment noKztGarment = new DesignGarment();
        noKztGarment.setDesign(design);
        noKztGarment.setGarmentType(GarmentType.T_SHIRT);
        noKztGarment.setActive(true);
        noKztGarment = designGarmentRepository.save(noKztGarment);
        noKztGarmentId = noKztGarment.getId();
        // intentionally no price row
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Legacy product path: pricing from productRepository, weight from heuristic formula.
     * qty=2 → estimateWeightGrams(2) = max(100, 250 + 2×150) = 550 g
     * stub fee = 1500 + 400 × 0.55 = 1 720.00 KZT
     */
    @Test
    void legacyProduct_returnsCorrectPriceAndHeuristicWeight() throws Exception {
        String body = """
                {
                  "toCityCode": %d,
                  "items": [{ "productId": %d, "quantity": 2 }]
                }
                """.formatted(CDEK_STUB_CITY, productId);

        mockMvc.perform(post("/api/v1/delivery/cdek/calculate-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemsTotal").value(10000.00))
                .andExpect(jsonPath("$.estimatedWeightGrams").value(550))
                .andExpect(jsonPath("$.deliveryPrice").value(1720.00))
                .andExpect(jsonPath("$.orderTotal").value(11720.00))
                .andExpect(jsonPath("$.currency").value("KZT"))
                .andExpect(jsonPath("$.sourcedFromStub").value(true));
    }

    /**
     * Design-garment path: pricing from designGarmentPriceRepository (KZT),
     * weight from GarmentWeightService.weightForType(HOODIE) — same source as order creation.
     * HOODIE default = 1.000 kg = 1000 g
     * stub fee = 1500 + 400 × 1.00 = 1 900.00 KZT
     */
    @Test
    void designGarment_returnsGarmentPriceAndActualWeight() throws Exception {
        String body = """
                {
                  "toCityCode": %d,
                  "items": [{ "designGarmentId": %d, "quantity": 1 }]
                }
                """.formatted(CDEK_STUB_CITY, hoodieGarmentId);

        mockMvc.perform(post("/api/v1/delivery/cdek/calculate-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemsTotal").value(12000.00))
                .andExpect(jsonPath("$.estimatedWeightGrams").value(1000))
                .andExpect(jsonPath("$.deliveryPrice").value(1900.00))
                .andExpect(jsonPath("$.orderTotal").value(13900.00))
                .andExpect(jsonPath("$.sourcedFromStub").value(true));
    }

    /**
     * Mixed cart: one legacy product (qty=1) and one HOODIE (qty=1).
     * product weight: estimateWeightGrams(1) = max(100, 250+150) = 400 g
     * hoodie weight: 1 000 g
     * total weight: 1 400 g → stub fee = 1500 + 400 × 1.40 = 2 060.00 KZT
     * itemsTotal: 5 000 + 12 000 = 17 000.00 KZT
     */
    @Test
    void mixedCart_combinesProductHeuristicAndGarmentActualWeight() throws Exception {
        String body = """
                {
                  "toCityCode": %d,
                  "items": [
                    { "productId": %d,        "quantity": 1 },
                    { "designGarmentId": %d,  "quantity": 1 }
                  ]
                }
                """.formatted(CDEK_STUB_CITY, productId, hoodieGarmentId);

        mockMvc.perform(post("/api/v1/delivery/cdek/calculate-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemsTotal").value(17000.00))
                .andExpect(jsonPath("$.estimatedWeightGrams").value(1400))
                .andExpect(jsonPath("$.deliveryPrice").value(2060.00))
                .andExpect(jsonPath("$.orderTotal").value(19060.00))
                .andExpect(jsonPath("$.sourcedFromStub").value(true));
    }

    /**
     * Item with neither productId nor designGarmentId must be rejected immediately.
     */
    @Test
    void noItemIdentifier_returnsBadRequest() throws Exception {
        String body = """
                {
                  "toCityCode": %d,
                  "items": [{ "quantity": 1 }]
                }
                """.formatted(CDEK_STUB_CITY);

        mockMvc.perform(post("/api/v1/delivery/cdek/calculate-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Design garment that exists but has no KZT price must return 400 Business Rule error.
     */
    @Test
    void designGarmentWithoutKztPrice_returnsBadRequest() throws Exception {
        String body = """
                {
                  "toCityCode": %d,
                  "items": [{ "designGarmentId": %d, "quantity": 1 }]
                }
                """.formatted(CDEK_STUB_CITY, noKztGarmentId);

        mockMvc.perform(post("/api/v1/delivery/cdek/calculate-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Non-existent design garment ID must return 404.
     */
    @Test
    void nonExistentDesignGarment_returnsNotFound() throws Exception {
        String body = """
                {
                  "toCityCode": %d,
                  "items": [{ "designGarmentId": 999999, "quantity": 1 }]
                }
                """.formatted(CDEK_STUB_CITY);

        mockMvc.perform(post("/api/v1/delivery/cdek/calculate-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
