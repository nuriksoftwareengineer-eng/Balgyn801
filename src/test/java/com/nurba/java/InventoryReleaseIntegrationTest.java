package com.nurba.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Color;
import com.nurba.java.domain.Design;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.DesignGarmentPrice;
import com.nurba.java.domain.Inventory;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.Payment;
import com.nurba.java.domain.Size;
import com.nurba.java.dto.request.UpdateOrderStatusRequest;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.GarmentType;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.enums.PaymentStatus;
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
import com.nurba.java.repositories.ProcessedWebhookEventRepository;
import com.nurba.java.repositories.SizeRepository;
import com.nurba.java.security.webhook.PaymentRateLimiterFilter;
import com.nurba.java.security.webhook.WebhookSignatureFilter;
import com.nurba.java.service.OrderService;
import com.nurba.java.service.Impl.OrderExpiryService;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for inventory release correctness across three paths:
 * <ol>
 *   <li>Failed / cancelled payment webhook → immediate release (Priority 1 &amp; 2)</li>
 *   <li>Scheduled order expiry → PENDING payments cancelled (Priority 3-expiry)</li>
 *   <li>Admin order cancellation → inventory restored + payments cancelled (Priority 3-admin)</li>
 * </ol>
 *
 * Uses bypass HMAC mode (secrets are empty in application-test.properties) so webhook
 * requests need no signature header.
 */
@SpringBootTest
@ActiveProfiles("test")
class InventoryReleaseIntegrationTest {

    private static final String PROVIDER     = "KASPI";
    private static final String WEBHOOK_URL  = "/api/v1/payments/webhook/" + PROVIDER;
    private static final String INIT_URL     = "/api/v1/payments/init";
    private static final String ORDER_URL    = "/api/v1/order";

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;
    @Autowired private PaymentRateLimiterFilter rateLimiterFilter;
    @Autowired private WebhookSignatureFilter   signatureFilter;

    @Autowired private OrderExpiryService  orderExpiryService;
    @Autowired private OrderService        orderService;

    @Autowired private CatalogGroupRepository          catalogGroupRepository;
    @Autowired private CollectionRepository            collectionRepository;
    @Autowired private DesignRepository                designRepository;
    @Autowired private ColorRepository                 colorRepository;
    @Autowired private SizeRepository                  sizeRepository;
    @Autowired private DesignGarmentRepository         designGarmentRepository;
    @Autowired private DesignGarmentPriceRepository    designGarmentPriceRepository;
    @Autowired private InventoryRepository             inventoryRepository;
    @Autowired private OrderRepository                 orderRepository;
    @Autowired private OrderItemRepository             orderItemRepository;
    @Autowired private OrderHistoryRepository          orderHistoryRepository;
    @Autowired private PaymentRepository               paymentRepository;
    @Autowired private ProcessedWebhookEventRepository processedEventRepository;
    @Autowired private CustomerRepository              customerRepository;

    private Long garmentId;
    private Long colorId;
    private Long sizeId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(rateLimiterFilter, signatureFilter)
                .build();
        cleanAll();
        buildFixture();
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Priority 1 & 2: failed/cancelled payment webhook releases inventory immediately
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void failedPaymentWebhook_releasesInventoryImmediatelyAndExpiresOrder() throws Exception {
        saveInventory(5);
        long orderId   = createOrder(2);
        long paymentId = initPayment(orderId);

        assertThat(currentInventoryQty()).isEqualTo(3);   // 2 units reserved at creation

        // POST a FAILED webhook — no signature needed (bypass mode)
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody(paymentId, "failed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        // Inventory must be restored immediately — not after a 60-min expiry window
        assertThat(currentInventoryQty()).isEqualTo(5);

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);

        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void cancelledPaymentWebhook_releasesInventoryImmediatelyAndExpiresOrder() throws Exception {
        saveInventory(5);
        long orderId   = createOrder(2);
        long paymentId = initPayment(orderId);

        // "declined" → PaymentStatus.CANCELLED for KASPI
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody(paymentId, "declined")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(currentInventoryQty()).isEqualTo(5);

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
    }

    @Test
    void failedPaymentWebhook_onAlreadyConfirmedOrder_doesNotExpireOrder() throws Exception {
        // An out-of-order FAILED webhook arriving after order was already CONFIRMED must
        // update the payment status but leave the CONFIRMED order untouched.
        saveInventory(5);
        long orderId = createOrder(2);

        // Directly promote order to CONFIRMED (simulate prior successful payment)
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // Now create a second payment (first one conceptually succeeded, this is a second attempt)
        long paymentId2 = initPayment(orderId);

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody(paymentId2, "failed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        // Order must remain CONFIRMED — a late FAILED webhook must not expire a confirmed order
        Order after = orderRepository.findById(orderId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        // Inventory should NOT have been released (order is active)
        assertThat(currentInventoryQty()).isEqualTo(3);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Priority 3 (expiry path): order expiry cancels PENDING payments
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void orderExpiry_cancelsPendingPaymentAndReleasesInventory() throws Exception {
        saveInventory(5);
        long orderId   = createOrder(2);
        long paymentId = initPayment(orderId);

        Payment before = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(before.getStatus()).isEqualTo(PaymentStatus.PENDING);

        backdate(orderId, 120);   // make order appear 2 hours old
        orderExpiryService.expireStaleOrders();

        Payment after = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(PaymentStatus.CANCELLED);

        assertThat(currentInventoryQty()).isEqualTo(5);

        Order expired = orderRepository.findById(orderId).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(OrderStatus.EXPIRED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Priority 3 (admin path): admin cancellation of a paid order restores inventory
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void adminCancelsConfirmedOrder_releasesInventory() throws Exception {
        saveInventory(5);
        long orderId = createOrder(2);
        assertThat(currentInventoryQty()).isEqualTo(3);

        // Simulate payment success by directly promoting the order to CONFIRMED
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        orderService.updateOrderStatus(orderId, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));

        assertThat(currentInventoryQty()).isEqualTo(5);

        Order cancelled = orderRepository.findById(orderId).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void adminCancelsPendingOrder_releasesInventoryAndCancelsPendingPayment() throws Exception {
        saveInventory(5);
        long orderId   = createOrder(2);
        long paymentId = initPayment(orderId);

        orderService.updateOrderStatus(orderId, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));

        assertThat(currentInventoryQty()).isEqualTo(5);

        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void adminCancelTwice_doesNotDoubleReleaseInventory() throws Exception {
        saveInventory(5);
        long orderId = createOrder(2);
        assertThat(currentInventoryQty()).isEqualTo(3);

        orderService.updateOrderStatus(orderId, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));
        assertThat(currentInventoryQty()).isEqualTo(5);

        // Second cancel (CANCELLED → CANCELLED is allowed but must be a no-op for inventory)
        orderService.updateOrderStatus(orderId, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));
        assertThat(currentInventoryQty()).isEqualTo(5);   // must still be 5, not 7
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private long createOrder(int quantity) throws Exception {
        String body = """
                {
                  "customerName": "Release Test",
                  "customerPhone": "+77001112233",
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

        MvcResult result = mockMvc.perform(post(ORDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long initPayment(long orderId) throws Exception {
        String body = """
                {"orderId":%d,"provider":"KASPI"}
                """.formatted(orderId).trim();

        MvcResult result = mockMvc.perform(post(INIT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private static String webhookBody(long paymentId, String status) {
        return """
                {"paymentId":%d,"status":"%s"}
                """.formatted(paymentId, status).trim();
    }

    private void backdate(long orderId, int minutesAgo) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setCreatedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        orderRepository.save(order);
    }

    private void saveInventory(int quantity) {
        DesignGarment garment = designGarmentRepository.findById(garmentId).orElseThrow();
        Color color           = colorRepository.findById(colorId).orElseThrow();
        Size size             = sizeRepository.findById(sizeId).orElseThrow();
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

    // ─────────────────────────────────────────────────────────────────────────
    // Fixture
    // ─────────────────────────────────────────────────────────────────────────

    private void cleanAll() {
        processedEventRepository.deleteAll();
        orderHistoryRepository.deleteAll();
        paymentRepository.deleteAll();
        orderItemRepository.deleteAll();
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
        group.setName("Release Group");
        group.setSlug("rel-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("Release Collection");
        coll.setSlug("rel-collection");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("Release Design");
        design.setSlug("rel-design");
        design.setActive(true);
        design.setCreatedAt(LocalDateTime.now());
        design = designRepository.save(design);

        Color color = new Color();
        color.setName("Blue");
        color.setHexCode("#0000FF");
        color = colorRepository.save(color);
        colorId = color.getId();

        Size size = new Size();
        size.setLabel("XL");
        size = sizeRepository.save(size);
        sizeId = size.getId();

        DesignGarment garment = new DesignGarment();
        garment.setDesign(design);
        garment.setGarmentType(GarmentType.SWEATSHIRT);
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
    }
}
