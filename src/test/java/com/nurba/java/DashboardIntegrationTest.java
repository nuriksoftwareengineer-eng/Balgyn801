package com.nurba.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Color;
import com.nurba.java.domain.Design;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.DesignGarmentPrice;
import com.nurba.java.domain.GarmentProfile;
import com.nurba.java.domain.Inventory;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.Size;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.DesignStatus;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.repositories.AppUserRepository;
import com.nurba.java.repositories.CatalogGroupRepository;
import com.nurba.java.repositories.GarmentProfileRepository;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.ColorRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integration tests for Feature 17 — admin dashboard analytics. */
@SpringBootTest
@ActiveProfiles("test")
class DashboardIntegrationTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

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
    @Autowired private GarmentProfileRepository garmentProfileRepository;

    private GarmentProfile garmentProfile;
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
        garmentProfileRepository.deleteAll();
    }

    private void buildFixture() {
        CatalogGroup group = new CatalogGroup();
        group.setName("Dashboard Group");
        group.setSlug("dashboard-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("Dashboard Collection");
        coll.setSlug("dashboard-coll");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("Dashboard Design");
        design.setSlug("dashboard-design");
        design.setStatus(DesignStatus.PUBLISHED);
        design.setCreatedAt(LocalDateTime.now());
        design = designRepository.save(design);

        GarmentProfile gp = new GarmentProfile();
        gp.setName("Test Profile");
        gp.setWeightKg(new BigDecimal("0.500"));
        gp.setLengthCm(35);
        gp.setWidthCm(28);
        gp.setHeightCm(8);
        gp.setSortOrder(0);
        garmentProfile = garmentProfileRepository.save(gp);

        Color color = new Color();
        color.setName("Green");
        color.setHexCode("#00FF00");
        color = colorRepository.save(color);
        colorId = color.getId();

        Size size = new Size();
        size.setLabel("L");
        size = sizeRepository.save(size);
        sizeId = size.getId();

        DesignGarment garment = new DesignGarment();
        garment.setDesign(design);
        garment.setGarmentProfile(garmentProfile);
        garment.setActive(true);
        garment.getColors().add(color);
        garment.getSizes().add(size);
        garment = designGarmentRepository.save(garment);
        garmentId = garment.getId();

        DesignGarmentPrice price = new DesignGarmentPrice();
        price.setDesignGarment(garment);
        price.setCurrency(Currency.KZT);
        price.setAmount(new BigDecimal("7000.00"));
        designGarmentPriceRepository.save(price);

        Inventory inv = new Inventory();
        inv.setDesignGarment(garment);
        inv.setColor(color);
        inv.setSize(size);
        inv.setQuantity(20);
        inventoryRepository.save(inv);
    }

    private long createOrder() throws Exception {
        String body = """
                {
                  "customerName": "Dashboard Test",
                  "customerPhone": "+77005556677",
                  "deliveryType": "PICKUP",
                  "items": [{
                    "designGarmentId": %d,
                    "colorId": %d,
                    "sizeId": %d,
                    "currency": "KZT",
                    "quantity": 1
                  }]
                }
                """.formatted(garmentId, colorId, sizeId);

        MvcResult result = mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void nonAdmin_cannotAccessStats() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stats_countsOrdersAndRevenueForConfirmedOrders() throws Exception {
        long orderId1 = createOrder();
        long orderId2 = createOrder();

        // Only CONFIRMED+ orders count as revenue (PAID_STATUSES) — confirm both.
        Order o1 = orderRepository.findById(orderId1).orElseThrow();
        o1.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(o1);
        Order o2 = orderRepository.findById(orderId2).orElseThrow();
        o2.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(o2);

        MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard/stats").param("days", "30")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode stats = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(stats.get("totalOrders").asLong()).isEqualTo(2);
        assertThat(stats.get("revenueTotal").asDouble()).isEqualTo(14000.0); // 2 * 7000
        assertThat(stats.has("dailyRevenue")).isTrue();
        assertThat(stats.get("dailyRevenue").isArray()).isTrue();
        assertThat(stats.get("ordersByStatus").isArray()).isTrue();
        assertThat(stats.get("topDesigns").isArray()).isTrue();
    }

    @Test
    void stats_pendingPaymentOrders_excludedFromRevenue() throws Exception {
        createOrder(); // stays PENDING_PAYMENT — must not count toward paid revenue

        MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode stats = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(stats.get("revenueTotal").asDouble()).isEqualTo(0.0);
    }

    @Test
    void stats_daysParam_isCappedAt365() throws Exception {
        // Just verifies the endpoint accepts an out-of-range value without error (capped server-side).
        mockMvc.perform(get("/api/v1/admin/dashboard/stats").param("days", "99999")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
