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
import com.nurba.java.domain.OrderHistory;
import com.nurba.java.domain.Size;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.repositories.AppUserRepository;
import com.nurba.java.repositories.CatalogGroupRepository;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.ColorRepository;
import com.nurba.java.repositories.CustomerRepository;
import com.nurba.java.repositories.DesignGarmentPriceRepository;
import com.nurba.java.repositories.DesignGarmentRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.repositories.GarmentProfileRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class OrderHistoryIntegrationTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    // Catalog fixture repos
    @Autowired private CatalogGroupRepository catalogGroupRepository;
    @Autowired private CollectionRepository collectionRepository;
    @Autowired private DesignRepository designRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private DesignGarmentRepository designGarmentRepository;
    @Autowired private DesignGarmentPriceRepository designGarmentPriceRepository;
    @Autowired private InventoryRepository inventoryRepository;

    // Verification + cleanup repos
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

    // ── Helpers ──────────────────────────────────────────────────────────────

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
        group.setName("History Group");
        group.setSlug("history-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("History Collection");
        coll.setSlug("history-coll");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("History Design");
        design.setSlug("history-design");
        design.setStatus(com.nurba.java.enums.DesignStatus.PUBLISHED);
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
        color.setName("Blue");
        color.setHexCode("#0000FF");
        color = colorRepository.save(color);
        colorId = color.getId();

        Size size = new Size();
        size.setLabel("S");
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
        price.setAmount(new BigDecimal("8000.00"));
        designGarmentPriceRepository.save(price);

        Inventory inv = new Inventory();
        inv.setDesignGarment(garment);
        inv.setColor(color);
        inv.setSize(size);
        inv.setQuantity(5);
        inventoryRepository.save(inv);
    }

    /** Creates an order anonymously and returns its ID. */
    private long createOrder() throws Exception {
        String body = """
                {
                  "customerName": "History Test",
                  "customerPhone": "+77009001122",
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

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void createOrder_writesNewHistoryEntry() throws Exception {
        long orderId = createOrder();

        List<OrderHistory> history =
                orderHistoryRepository.findByOrder_IdOrderByDateAddedDesc(orderId);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(history.get(0).getDateAdded()).isNotNull();
    }

    @Test
    void updateStatus_appendsNewHistoryEntry() throws Exception {
        long orderId = createOrder();

        // Simulate payment confirmation: system sets CONFIRMED, admin cannot do this directly.
        // (PENDING_PAYMENT → CONFIRMED is a system-only transition via payment callback/capture.)
        com.nurba.java.domain.Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Admin moves CONFIRMED → IN_PRODUCTION (valid admin transition)
        mockMvc.perform(patch("/api/v1/order/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IN_PRODUCTION\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());

        List<OrderHistory> history =
                orderHistoryRepository.findByOrder_IdOrderByDateAddedDesc(orderId);

        // Two entries: IN_PRODUCTION (latest) and PENDING_PAYMENT (creation)
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
        assertThat(history.get(1).getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void multipleStatusUpdates_allRecorded() throws Exception {
        long orderId = createOrder();

        // Simulate payment: CONFIRMED set directly (system-side, not admin)
        com.nurba.java.domain.Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Admin moves through production pipeline: CONFIRMED → IN_PRODUCTION → READY → SHIPPED
        for (String status : List.of("IN_PRODUCTION", "READY", "SHIPPED")) {
            mockMvc.perform(patch("/api/v1/order/" + orderId + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"" + status + "\"}")
                            .with(SecurityMockMvcRequestPostProcessors.user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk());
        }

        List<OrderHistory> history =
                orderHistoryRepository.findByOrder_IdOrderByDateAddedDesc(orderId);

        // 1 creation (PENDING_PAYMENT) + 3 admin transitions = 4 entries
        assertThat(history).hasSize(4);
        assertThat(history.get(0).getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(history.get(3).getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }
}
