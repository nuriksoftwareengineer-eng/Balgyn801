package com.nurba.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Color;
import com.nurba.java.domain.Design;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.DesignGarmentPrice;
import com.nurba.java.domain.Inventory;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.OrderHistory;
import com.nurba.java.domain.Size;
import com.nurba.java.dto.responce.OrderResponse;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.GarmentType;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.repositories.CatalogGroupRepository;
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
import com.nurba.java.service.OrderService;
import com.nurba.java.service.Impl.OrderExpiryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 3 — unpaid orders expire after the payment window: inventory is released, the order
 * becomes EXPIRED, it stays in the audit log, and it never appears in the admin list.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderExpiryIntegrationTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrderExpiryService orderExpiryService;
    @Autowired private OrderService orderService;

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

    private Long garmentId;
    private Long colorId;
    private Long sizeId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        cleanAll();
        buildFixture();
    }

    private void cleanAll() {
        orderHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();
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
        group.setName("Expiry Group");
        group.setSlug("exp-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("Expiry Collection");
        coll.setSlug("exp-collection");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("Expiry Design");
        design.setSlug("exp-design");
        design.setActive(true);
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
        garment.setGarmentType(GarmentType.HOODIE);
        garment.setActive(true);
        garment.getColors().add(color);
        garment.getSizes().add(size);
        garment = designGarmentRepository.save(garment);
        garmentId = garment.getId();

        DesignGarmentPrice price = new DesignGarmentPrice();
        price.setDesignGarment(garment);
        price.setCurrency(Currency.KZT);
        price.setAmount(new BigDecimal("12000.00"));
        designGarmentPriceRepository.save(price);
    }

    private void saveInventory(int quantity) {
        DesignGarment garment = designGarmentRepository.findById(garmentId).orElseThrow();
        Color color = colorRepository.findById(colorId).orElseThrow();
        Size size = sizeRepository.findById(sizeId).orElseThrow();
        Inventory inv = new Inventory();
        inv.setDesignGarment(garment);
        inv.setColor(color);
        inv.setSize(size);
        inv.setQuantity(quantity);
        inventoryRepository.save(inv);
    }

    private int currentInventoryQty() {
        return inventoryRepository
                .findByDesignGarment_IdAndColor_IdAndSize_Id(garmentId, colorId, sizeId)
                .map(Inventory::getQuantity)
                .orElseThrow();
    }

    private long createOrder(int quantity) throws Exception {
        String body = """
                {
                  "customerName": "Expiry Test",
                  "customerPhone": "+77008888888",
                  "deliveryType": "PICKUP",
                  "items": [{
                    "designGarmentId": %d,
                    "colorId": %d,
                    "sizeId": %d,
                    "currency": "KZT",
                    "quantity": %d
                  }]
                }
                """.formatted(garmentId, colorId, sizeId, quantity);

        MvcResult result = mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private void backdate(long orderId, int minutesAgo) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setCreatedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        orderRepository.save(order);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void staleUnpaidOrder_isExpired_inventoryReleased_andHiddenFromAdmin() throws Exception {
        saveInventory(5);
        long orderId = createOrder(2);
        assertThat(currentInventoryQty()).isEqualTo(3);          // reserved at creation

        backdate(orderId, 120);                                  // 2 hours old → past the 60-min window
        orderExpiryService.expireStaleOrders();

        Order expired = orderRepository.findById(orderId).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(currentInventoryQty()).isEqualTo(5);          // released back to stock

        // Excluded from admin list
        assertThat(orderService.getAll()).extracting(OrderResponse::getId).doesNotContain(orderId);

        // Retained in audit log with an EXPIRED entry
        List<OrderHistory> history = orderHistoryRepository.findByOrder_IdOrderByDateAddedDesc(orderId);
        assertThat(history).extracting(OrderHistory::getStatus).contains(OrderStatus.EXPIRED);
    }

    @Test
    void freshUnpaidOrder_isNotExpired() throws Exception {
        saveInventory(5);
        long orderId = createOrder(2);

        orderExpiryService.expireStaleOrders();                  // order is brand new → untouched

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(currentInventoryQty()).isEqualTo(3);          // still reserved
    }
}
