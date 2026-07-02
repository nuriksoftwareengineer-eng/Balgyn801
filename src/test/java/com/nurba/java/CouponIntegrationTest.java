package com.nurba.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Color;
import com.nurba.java.domain.Coupon;
import com.nurba.java.domain.Design;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.DesignGarmentPrice;
import com.nurba.java.domain.Inventory;
import com.nurba.java.domain.Size;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.DesignStatus;
import com.nurba.java.enums.DiscountType;
import com.nurba.java.enums.GarmentType;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.repositories.AppUserRepository;
import com.nurba.java.repositories.CatalogGroupRepository;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.ColorRepository;
import com.nurba.java.repositories.CouponRepository;
import com.nurba.java.repositories.CustomerRepository;
import com.nurba.java.repositories.DesignGarmentPriceRepository;
import com.nurba.java.repositories.DesignGarmentRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.repositories.InventoryRepository;
import com.nurba.java.repositories.OrderHistoryRepository;
import com.nurba.java.repositories.OrderItemRepository;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.repositories.PaymentRepository;
import com.nurba.java.repositories.SizeRepository;
import com.nurba.java.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integration + concurrency tests for Feature 14 — Coupons. */
@SpringBootTest
@ActiveProfiles("test")
class CouponIntegrationTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;
    @Autowired private CouponRepository couponRepository;
    @Autowired private CouponService couponService;

    // Catalog fixture (for the order-application test)
    @Autowired private CatalogGroupRepository catalogGroupRepository;
    @Autowired private CollectionRepository collectionRepository;
    @Autowired private DesignRepository designRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private DesignGarmentRepository designGarmentRepository;
    @Autowired private DesignGarmentPriceRepository designGarmentPriceRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private OrderHistoryRepository orderHistoryRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AppUserRepository appUserRepository;

    private Long garmentId;
    private Long colorId;
    private Long sizeId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        cleanAll();
        buildFixture();
    }

    private void cleanAll() {
        couponRepository.deleteAll();
        orderHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();
        appUserRepository.deleteAll();
        inventoryRepository.deleteAll();
        designGarmentPriceRepository.deleteAll();
        designGarmentRepository.deleteAll();
        designRepository.deleteAll();
        collectionRepository.deleteAll();
        catalogGroupRepository.deleteAll();
        colorRepository.deleteAll();
        sizeRepository.deleteAll();
    }

    private void buildFixture() {
        CatalogGroup group = new CatalogGroup();
        group.setName("Coupon Group");
        group.setSlug("coupon-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("Coupon Collection");
        coll.setSlug("coupon-coll");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("Coupon Design");
        design.setSlug("coupon-design");
        design.setStatus(DesignStatus.PUBLISHED);
        design.setCreatedAt(LocalDateTime.now());
        design = designRepository.save(design);

        Color color = new Color();
        color.setName("Black");
        color.setHexCode("#000000");
        color = colorRepository.save(color);
        colorId = color.getId();

        Size size = new Size();
        size.setLabel("M");
        size = sizeRepository.save(size);
        sizeId = size.getId();

        DesignGarment garment = new DesignGarment();
        garment.setDesign(design);
        garment.setGarmentType(GarmentType.T_SHIRT);
        garment.setActive(true);
        garment.getColors().add(color);
        garment.getSizes().add(size);
        garment = designGarmentRepository.save(garment);
        garmentId = garment.getId();

        DesignGarmentPrice price = new DesignGarmentPrice();
        price.setDesignGarment(garment);
        price.setCurrency(Currency.KZT);
        price.setAmount(new BigDecimal("10000.00"));
        designGarmentPriceRepository.save(price);

        Inventory inv = new Inventory();
        inv.setDesignGarment(garment);
        inv.setColor(color);
        inv.setSize(size);
        inv.setQuantity(50);
        inventoryRepository.save(inv);
    }

    private Coupon saveCoupon(String code, DiscountType type, BigDecimal value,
                               BigDecimal minOrder, Integer maxUses, boolean active, LocalDateTime expiresAt) {
        Coupon c = new Coupon();
        c.setCode(code);
        c.setDiscountType(type);
        c.setDiscountValue(value);
        c.setMinOrderAmount(minOrder != null ? minOrder : BigDecimal.ZERO);
        c.setMaxUses(maxUses);
        c.setUsedCount(0);
        c.setActive(active);
        c.setExpiresAt(expiresAt);
        c.setCreatedAt(LocalDateTime.now());
        return couponRepository.save(c);
    }

    private String orderBodyWithCoupon(String couponCode) {
        return """
                {
                  "customerName": "Coupon Test",
                  "customerPhone": "+77001112233",
                  "deliveryType": "PICKUP",
                  "couponCode": "%s",
                  "items": [{
                    "designGarmentId": %d,
                    "colorId": %d,
                    "sizeId": %d,
                    "currency": "KZT",
                    "quantity": 1
                  }]
                }
                """.formatted(couponCode, garmentId, colorId, sizeId);
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    @Test
    void adminCreate_thenList_returnsCoupon() throws Exception {
        String body = """
                {"code":"WELCOME10","discountType":"PERCENTAGE","discountValue":10,"minOrderAmount":0,"active":true}
                """;
        mockMvc.perform(post("/api/v1/admin/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/v1/admin/coupons")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode page = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(page.get("content").size()).isEqualTo(1);
        assertThat(page.get("content").get(0).get("code").asText()).isEqualTo("WELCOME10");
    }

    @Test
    void adminCreate_duplicateCode_rejected() throws Exception {
        saveCoupon("DUP10", DiscountType.PERCENTAGE, new BigDecimal("10"), null, null, true, null);
        String body = """
                {"code":"DUP10","discountType":"PERCENTAGE","discountValue":15,"minOrderAmount":0,"active":true}
                """;
        mockMvc.perform(post("/api/v1/admin/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminUpdate_changesFields() throws Exception {
        Coupon c = saveCoupon("UPD10", DiscountType.PERCENTAGE, new BigDecimal("10"), null, null, true, null);
        String body = """
                {"code":"UPD10","discountType":"FIXED","discountValue":500,"minOrderAmount":0,"active":false}
                """;
        mockMvc.perform(put("/api/v1/admin/coupons/" + c.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.discountType").value("FIXED"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.active").value(false));
    }

    @Test
    void adminDelete_removesCoupon() throws Exception {
        Coupon c = saveCoupon("DEL10", DiscountType.PERCENTAGE, new BigDecimal("10"), null, null, true, null);
        mockMvc.perform(delete("/api/v1/admin/coupons/" + c.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isNoContent());
        assertThat(couponRepository.findById(c.getId())).isEmpty();
    }

    @Test
    void nonAdmin_cannotManageCoupons() throws Exception {
        mockMvc.perform(get("/api/v1/admin/coupons"))
                .andExpect(status().isUnauthorized());
    }

    // ── Public validate ───────────────────────────────────────────────────────

    @Test
    void validate_percentageCoupon_computesDiscount() throws Exception {
        saveCoupon("PCT10", DiscountType.PERCENTAGE, new BigDecimal("10"), null, null, true, null);

        MvcResult result = mockMvc.perform(get("/api/v1/coupons/validate")
                        .param("code", "PCT10")
                        .param("orderTotal", "10000"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("discountAmount").asDouble()).isEqualTo(1000.0);
        assertThat(json.get("finalTotal").asDouble()).isEqualTo(9000.0);
    }

    @Test
    void validate_fixedCoupon_neverExceedsTotal() throws Exception {
        saveCoupon("FIX5000", DiscountType.FIXED, new BigDecimal("5000"), null, null, true, null);

        MvcResult result = mockMvc.perform(get("/api/v1/coupons/validate")
                        .param("code", "FIX5000")
                        .param("orderTotal", "3000"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        // Discount capped at order total — never goes negative.
        assertThat(json.get("discountAmount").asDouble()).isEqualTo(3000.0);
        assertThat(json.get("finalTotal").asDouble()).isEqualTo(0.0);
    }

    @Test
    void validate_expiredCoupon_rejected() throws Exception {
        saveCoupon("OLD10", DiscountType.PERCENTAGE, new BigDecimal("10"), null, null, true,
                LocalDateTime.now().minusDays(1));

        mockMvc.perform(get("/api/v1/coupons/validate")
                        .param("code", "OLD10")
                        .param("orderTotal", "10000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validate_inactiveCoupon_rejected() throws Exception {
        saveCoupon("OFF10", DiscountType.PERCENTAGE, new BigDecimal("10"), null, null, false, null);

        mockMvc.perform(get("/api/v1/coupons/validate")
                        .param("code", "OFF10")
                        .param("orderTotal", "10000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validate_belowMinOrderAmount_rejected() throws Exception {
        saveCoupon("MIN5000", DiscountType.PERCENTAGE, new BigDecimal("10"),
                new BigDecimal("5000"), null, true, null);

        mockMvc.perform(get("/api/v1/coupons/validate")
                        .param("code", "MIN5000")
                        .param("orderTotal", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validate_maxUsesReached_rejected() throws Exception {
        Coupon c = saveCoupon("MAXED", DiscountType.PERCENTAGE, new BigDecimal("10"), null, 2, true, null);
        c.setUsedCount(2);
        couponRepository.save(c);

        mockMvc.perform(get("/api/v1/coupons/validate")
                        .param("code", "MAXED")
                        .param("orderTotal", "10000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validate_unknownCode_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/coupons/validate")
                        .param("code", "NOPE")
                        .param("orderTotal", "10000"))
                .andExpect(status().isBadRequest());
    }

    // ── Order application ─────────────────────────────────────────────────────

    @Test
    void createOrder_withCoupon_appliesDiscountAndIncrementsUsage() throws Exception {
        Coupon c = saveCoupon("ORDER10", DiscountType.PERCENTAGE, new BigDecimal("10"), null, null, true, null);

        MvcResult result = mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBodyWithCoupon("ORDER10")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode order = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(order.get("couponCode").asText()).isEqualTo("ORDER10");
        assertThat(order.get("discountAmount").asDouble()).isEqualTo(1000.0); // 10% of 10000
        assertThat(order.get("totalPrice").asDouble()).isEqualTo(9000.0);

        Coupon reloaded = couponRepository.findById(c.getId()).orElseThrow();
        assertThat(reloaded.getUsedCount()).isEqualTo(1);
    }

    @Test
    void createOrder_withInvalidCoupon_rejectsWholeOrder() throws Exception {
        mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBodyWithCoupon("DOESNOTEXIST")))
                .andExpect(status().isBadRequest());

        // No order, no inventory deduction — transaction rolled back entirely.
        assertThat(orderRepository.findAll()).isEmpty();
    }

    // ── Race condition fix (Task 4) ──────────────────────────────────────────

    @Test
    void incrementUsage_concurrentCalls_neverExceedsMaxUses() throws Exception {
        int maxUses = 5;
        int attempts = 25;
        Coupon c = saveCoupon("RACE5", DiscountType.PERCENTAGE, new BigDecimal("10"), null, maxUses, true, null);
        Long couponId = c.getId();

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(attempts);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                try {
                    couponService.incrementUsage(couponId);
                    succeeded.incrementAndGet();
                } catch (BusinessRuleException e) {
                    rejected.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        // Exactly maxUses increments succeed — the atomic UPDATE guard closes the race window.
        assertThat(succeeded.get()).isEqualTo(maxUses);
        assertThat(rejected.get()).isEqualTo(attempts - maxUses);

        Coupon reloaded = couponRepository.findById(couponId).orElseThrow();
        assertThat(reloaded.getUsedCount()).isEqualTo(maxUses);
    }
}
