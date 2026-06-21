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
import com.nurba.java.domain.Payment;
import com.nurba.java.domain.Size;
import com.nurba.java.dto.request.UpdateOrderStatusRequest;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.GarmentType;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.enums.PaymentStatus;
import com.nurba.java.payment.FreedomPaySignature;
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
import com.nurba.java.service.OrderService;
import com.nurba.java.service.Impl.OrderExpiryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * Integration tests for inventory release correctness across three paths:
 * <ol>
 *   <li>Failed payment callback → immediate release (Priority 1)</li>
 *   <li>Scheduled order expiry → PENDING payments cancelled (Priority 2)</li>
 *   <li>Admin order cancellation → inventory restored + payments cancelled (Priority 3)</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.payment.freedompay.merchant-id=",
        "app.payment.freedompay.secret-key=test-inv-release-secret-32chars!",
        "app.payment.freedompay.callback-script-name=freedom-pay",
        "app.security.rate-limit.trust-proxy=true"
})
class InventoryReleaseIntegrationTest {

    private static final String TEST_SECRET  = "test-inv-release-secret-32chars!";
    private static final String CALLBACK_URL = "/api/v1/payments/callback/freedom-pay";
    private static final String SCRIPT_NAME  = "freedom-pay";
    private static final String INIT_URL     = "/api/v1/payments/init";
    private static final String ORDER_URL    = "/api/v1/order";

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;
    @Autowired private PaymentRateLimiterFilter rateLimiterFilter;

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
                .addFilters(rateLimiterFilter)
                .build();
        cleanAll();
        buildFixture();
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Priority 1: failed payment callback releases inventory immediately
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void failedPaymentCallback_releasesInventoryImmediatelyAndExpiresOrder() throws Exception {
        saveInventory(5);
        long orderId = createOrder(2);
        assertThat(currentInventoryQty()).isEqualTo(3);

        InitResult init = initPayment(orderId);
        sendCallback(orderId, init.providerPaymentId(), "0", init.amount())
                .andExpect(xpath("//pg_status").string("ok"));

        assertThat(currentInventoryQty()).isEqualTo(5);

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);

        Payment payment = paymentRepository.findByProviderPaymentId(init.providerPaymentId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void failedPaymentCallback_onAlreadyConfirmedOrder_doesNotExpireOrder() throws Exception {
        saveInventory(5);
        long orderId = createOrder(2);

        // Simulate prior successful payment by directly setting CONFIRMED
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // A second payment attempt (for the same now-CONFIRMED order)
        InitResult init = initPayment(orderId);
        sendCallback(orderId, init.providerPaymentId(), "0", init.amount())
                .andExpect(xpath("//pg_status").string("ok"));

        // CONFIRMED order must not be expired by a late FAILED callback
        Order after = orderRepository.findById(orderId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(currentInventoryQty()).isEqualTo(3);  // inventory still reserved
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Priority 2: scheduled order expiry cancels PENDING payments
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void orderExpiry_cancelsPendingPaymentAndReleasesInventory() throws Exception {
        saveInventory(5);
        long orderId = createOrder(2);
        InitResult init = initPayment(orderId);

        Payment before = paymentRepository.findByProviderPaymentId(init.providerPaymentId()).orElseThrow();
        assertThat(before.getStatus()).isEqualTo(PaymentStatus.PENDING);

        backdate(orderId, 120);
        orderExpiryService.expireStaleOrders();

        Payment after = paymentRepository.findByProviderPaymentId(init.providerPaymentId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(currentInventoryQty()).isEqualTo(5);

        Order expired = orderRepository.findById(orderId).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(OrderStatus.EXPIRED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Priority 3: admin cancellation restores inventory
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void adminCancelsConfirmedOrder_releasesInventory() throws Exception {
        saveInventory(5);
        long orderId = createOrder(2);
        assertThat(currentInventoryQty()).isEqualTo(3);

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
        long orderId = createOrder(2);
        InitResult init = initPayment(orderId);

        orderService.updateOrderStatus(orderId, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));

        assertThat(currentInventoryQty()).isEqualTo(5);
        Payment payment = paymentRepository.findByProviderPaymentId(init.providerPaymentId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void adminCancelTwice_doesNotDoubleReleaseInventory() throws Exception {
        saveInventory(5);
        long orderId = createOrder(2);
        assertThat(currentInventoryQty()).isEqualTo(3);

        orderService.updateOrderStatus(orderId, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));
        assertThat(currentInventoryQty()).isEqualTo(5);

        orderService.updateOrderStatus(orderId, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));
        assertThat(currentInventoryQty()).isEqualTo(5);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fix 2: expire() is idempotent — double-call does not double-release
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void doubleExpire_doesNotDoubleReleaseInventory() throws Exception {
        saveInventory(5);
        long orderId = createOrder(2);
        assertThat(currentInventoryQty()).isEqualTo(3);

        Order order = orderRepository.findById(orderId).orElseThrow();

        // First expire: normal path — releases inventory, sets EXPIRED
        orderExpiryService.expire(order);
        assertThat(currentInventoryQty()).isEqualTo(5);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.EXPIRED);

        // Second expire: must be a no-op — order is already EXPIRED, inventory must not go to 7
        orderExpiryService.expire(order);
        assertThat(currentInventoryQty()).isEqualTo(5);
    }

    @Test
    void expireRace_callbackExpiresFirst_schedulerExpireIsNoOp() throws Exception {
        saveInventory(5);
        long orderId = createOrder(2);
        InitResult init = initPayment(orderId);
        assertThat(currentInventoryQty()).isEqualTo(3);

        // Simulate payment callback failing and expiring the order
        sendCallback(orderId, init.providerPaymentId(), "0", init.amount())
                .andExpect(xpath("//pg_status").string("ok"));
        assertThat(currentInventoryQty()).isEqualTo(5);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.EXPIRED);

        // Simulate scheduler arriving late with a stale Order reference
        Order staleRef = new Order();
        staleRef.setId(orderId);
        orderExpiryService.expire(staleRef);

        // Inventory must still be 5, not over-released to 7
        assertThat(currentInventoryQty()).isEqualTo(5);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fix 3: cancelling a SHIPPED or DELIVERED order must NOT release inventory
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void adminCancelsShippedOrder_doesNotReleaseInventory() throws Exception {
        saveInventory(5);
        long orderId = createOrder(2);
        assertThat(currentInventoryQty()).isEqualTo(3);

        // Simulate the order reaching SHIPPED status (goods physically dispatched)
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.SHIPPED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Admin cancels a SHIPPED order (e.g. parcel lost in transit after dispatch)
        orderService.updateOrderStatus(orderId, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));

        // Inventory must NOT be returned — goods already left the warehouse
        assertThat(currentInventoryQty()).isEqualTo(3);
        Order cancelled = orderRepository.findById(orderId).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void adminCancelsDeliveredOrder_doesNotReleaseInventory() throws Exception {
        saveInventory(5);
        long orderId = createOrder(2);
        assertThat(currentInventoryQty()).isEqualTo(3);

        // DELIVERED is a terminal state — cancellation must be blocked by the state machine.
        // This test verifies the allowlist enforces the terminal constraint.
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.DELIVERED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                orderService.updateOrderStatus(orderId, new UpdateOrderStatusRequest(OrderStatus.CANCELLED))
        ).isInstanceOf(com.nurba.java.exception.BusinessRuleException.class);

        // Inventory unchanged — DELIVERED transition was rejected
        assertThat(currentInventoryQty()).isEqualTo(3);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private record InitResult(long paymentId, String providerPaymentId, String amount) {}

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

    private InitResult initPayment(long orderId) throws Exception {
        String body = "{\"orderId\":" + orderId + "}";
        MvcResult result = mockMvc.perform(post(INIT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new InitResult(
                json.get("id").asLong(),
                json.get("providerPaymentId").asText(),
                json.get("amount").asText());
    }

    private org.springframework.test.web.servlet.ResultActions sendCallback(
            long orderId, String providerPaymentId, String pgResult, String amount) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("pg_order_id", String.valueOf(orderId));
        params.put("pg_payment_id", providerPaymentId);
        params.put("pg_amount", amount);
        params.put("pg_currency", "KZT");
        params.put("pg_result", pgResult);
        params.put("pg_description", "Test");
        params.put("pg_salt", "salt123");
        params.put("pg_sig", FreedomPaySignature.sign(SCRIPT_NAME, params, TEST_SECRET));

        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        params.forEach(mvm::add);

        return mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(mvm))
                .andExpect(status().isOk());
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
        design.setStatus(com.nurba.java.enums.DesignStatus.PUBLISHED);
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
