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
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.GarmentType;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;
import com.nurba.java.payment.PayPalOrdersClient;
import com.nurba.java.payment.PayPalWebhookVerifier;
import com.nurba.java.payment.dto.PayPalCaptureResponse;
import com.nurba.java.payment.dto.PayPalCreateOrderResponse;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the PayPal payment flow.
 *
 * PayPalOrdersClient and PayPalWebhookVerifier are mocked — no real PayPal API calls.
 * Covers: createOrder idempotency, captureOrder success/failure, webhook happy path,
 * replay protection, and invalid signature rejection.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.payment.paypal.mode=sandbox",
        "app.payment.paypal.client-id=test-client-id",
        "app.payment.paypal.client-secret=test-client-secret",
        "app.payment.paypal.webhook-id=test-webhook-id",
        "app.payment.freedompay.merchant-id=",
        "app.payment.freedompay.secret-key=test-fp-secret-32chars-longer!!",
        "app.security.rate-limit.trust-proxy=true"
})
class PayPalPaymentIntegrationTest {

    private static final String CREATE_ORDER_URL  = "/api/v1/payments/paypal/create-order";
    private static final String CAPTURE_URL       = "/api/v1/payments/paypal/capture/";
    private static final String WEBHOOK_URL       = "/api/v1/payments/paypal/webhook";

    private static final String FAKE_PAYPAL_ORDER_ID  = "PP-ORDER-TEST-001";
    private static final String FAKE_APPROVAL_URL     = "https://sandbox.paypal.com/checkoutnow?token=TEST";
    private static final String FAKE_CAPTURE_ID       = "CAP-TEST-001";

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;
    @Autowired private PaymentRateLimiterFilter rateLimiterFilter;

    @MockitoBean private PayPalOrdersClient payPalOrdersClient;
    @MockitoBean private PayPalWebhookVerifier payPalWebhookVerifier;

    @Autowired private CatalogGroupRepository catalogGroupRepository;
    @Autowired private CollectionRepository collectionRepository;
    @Autowired private DesignRepository designRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private DesignGarmentRepository designGarmentRepository;
    @Autowired private DesignGarmentPriceRepository designGarmentPriceRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private OrderHistoryRepository orderHistoryRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ProcessedWebhookEventRepository processedEventRepository;
    @Autowired private CustomerRepository customerRepository;

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

        // Default mock: createOrder returns a valid PayPal order
        PayPalCreateOrderResponse fakeOrder = new PayPalCreateOrderResponse(
                FAKE_PAYPAL_ORDER_ID,
                "CREATED",
                List.of(new PayPalCreateOrderResponse.Link(FAKE_APPROVAL_URL, "payer-action", "GET"))
        );
        when(payPalOrdersClient.createOrder(any(), anyString(), any(), any())).thenReturn(fakeOrder);

        // Default mock: captureOrder returns COMPLETED
        PayPalCaptureResponse.Capture capture = new PayPalCaptureResponse.Capture(FAKE_CAPTURE_ID, "COMPLETED", null);
        PayPalCaptureResponse.Payments payments = new PayPalCaptureResponse.Payments(List.of(capture));
        PayPalCaptureResponse.PurchaseUnit unit = new PayPalCaptureResponse.PurchaseUnit(payments);
        PayPalCaptureResponse fakeCapture = new PayPalCaptureResponse(
                FAKE_PAYPAL_ORDER_ID, "COMPLETED", List.of(unit));
        when(payPalOrdersClient.captureOrder(anyString())).thenReturn(fakeCapture);

        // Default mock: webhook signature valid
        when(payPalWebhookVerifier.verify(anyString(), any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. createOrder — returns approval URL
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createOrder_returnsApprovalUrl() throws Exception {
        long orderId = createOrder();

        mockMvc.perform(post(CREATE_ORDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("PAYPAL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentUrl").value(FAKE_APPROVAL_URL))
                .andExpect(jsonPath("$.providerPaymentId").value(FAKE_PAYPAL_ORDER_ID));

        assertThat(paymentRepository.findAll()).hasSize(1);
        Payment payment = paymentRepository.findAll().get(0);
        assertThat(payment.getProvider()).isEqualTo(PaymentProvider.PAYPAL);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getCurrency()).isEqualTo("USD");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. createOrder — idempotency: second call returns same payment
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createOrder_idempotent_returnsSamePayment() throws Exception {
        long orderId = createOrder();
        String body = "{\"orderId\":" + orderId + "}";

        MvcResult r1 = mockMvc.perform(post(CREATE_ORDER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();

        MvcResult r2 = mockMvc.perform(post(CREATE_ORDER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();

        long id1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asLong();
        long id2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("id").asLong();
        assertThat(id1).isEqualTo(id2);
        assertThat(paymentRepository.findAll()).hasSize(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. captureOrder — COMPLETED → Payment SUCCEEDED, Order CONFIRMED
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void captureOrder_completed_confirmsOrderAndPayment() throws Exception {
        long orderId = createOrder();
        // Init PayPal payment first
        mockMvc.perform(post(CREATE_ORDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post(CAPTURE_URL + FAKE_PAYPAL_ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        Payment payment = paymentRepository.findByProviderPaymentId(FAKE_PAYPAL_ORDER_ID).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. captureOrder — non-COMPLETED → Payment FAILED
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void captureOrder_denied_failsPayment() throws Exception {
        long orderId = createOrder();
        mockMvc.perform(post(CREATE_ORDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isOk());

        // Override mock: capture returns VOIDED (non-COMPLETED)
        PayPalCaptureResponse.PurchaseUnit unit = new PayPalCaptureResponse.PurchaseUnit(null);
        PayPalCaptureResponse deniedCapture = new PayPalCaptureResponse(
                FAKE_PAYPAL_ORDER_ID, "VOIDED", List.of(unit));
        when(payPalOrdersClient.captureOrder(anyString())).thenReturn(deniedCapture);

        mockMvc.perform(post(CAPTURE_URL + FAKE_PAYPAL_ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        Payment payment = paymentRepository.findByProviderPaymentId(FAKE_PAYPAL_ORDER_ID).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. webhook — PAYMENT.CAPTURE.COMPLETED confirms order
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void webhook_captureCompleted_confirmsOrder() throws Exception {
        long orderId = createOrder();
        mockMvc.perform(post(CREATE_ORDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isOk());

        String webhookBody = buildWebhookEvent("EVT-001", "PAYMENT.CAPTURE.COMPLETED", FAKE_PAYPAL_ORDER_ID);

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody))
                .andExpect(status().isOk());

        Payment payment = paymentRepository.findByProviderPaymentId(FAKE_PAYPAL_ORDER_ID).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        assertThat(processedEventRepository.existsByProviderAndEventId(
                PaymentProvider.PAYPAL, "EVT-001")).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. webhook — duplicate event is idempotent
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void webhook_duplicate_isIdempotent() throws Exception {
        long orderId = createOrder();
        mockMvc.perform(post(CREATE_ORDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isOk());

        String webhookBody = buildWebhookEvent("EVT-DUP", "PAYMENT.CAPTURE.COMPLETED", FAKE_PAYPAL_ORDER_ID);

        // First delivery
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody))
                .andExpect(status().isOk());

        // Second delivery — same eventId
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody))
                .andExpect(status().isOk());

        // Processed event recorded exactly once
        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. webhook — invalid signature → 400
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void captureOrder_calledTwice_isIdempotent_noDoubleCapture() throws Exception {
        long orderId = createOrder();
        mockMvc.perform(post(CREATE_ORDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isOk());

        // First capture — succeeds
        mockMvc.perform(post(CAPTURE_URL + FAKE_PAYPAL_ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        // Second capture — payment already SUCCEEDED → returns existing record, skips PayPal API
        mockMvc.perform(post(CAPTURE_URL + FAKE_PAYPAL_ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        // Still only one payment row, order confirmed
        assertThat(paymentRepository.findAll()).hasSize(1);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void captureOrder_onAlreadyConfirmedOrder_doesNotReConfirm() throws Exception {
        long orderId = createOrder();
        mockMvc.perform(post(CREATE_ORDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isOk());

        // Confirm via webhook first
        String webhookBody = buildWebhookEvent("EVT-PRECONF", "PAYMENT.CAPTURE.COMPLETED", FAKE_PAYPAL_ORDER_ID);
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody))
                .andExpect(status().isOk());

        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);

        // Second webhook with same order, different event ID — order must stay CONFIRMED
        String webhookBody2 = buildWebhookEvent("EVT-LATE", "PAYMENT.CAPTURE.COMPLETED", FAKE_PAYPAL_ORDER_ID);
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody2))
                .andExpect(status().isOk());

        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void createOrder_onCancelledOrder_returns400() throws Exception {
        long orderId = createOrder();

        // Manually cancel the order
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        mockMvc.perform(post(CREATE_ORDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhook_captureCompleted_withLinksFallback_confirmsOrder() throws Exception {
        long orderId = createOrder();
        mockMvc.perform(post(CREATE_ORDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isOk());

        // No supplementary_data — must use links[rel=up] fallback
        String webhookBody = buildWebhookEventWithLinksFallback(
                "EVT-LINK-001", "PAYMENT.CAPTURE.COMPLETED", FAKE_PAYPAL_ORDER_ID);
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody))
                .andExpect(status().isOk());

        Payment payment = paymentRepository.findByProviderPaymentId(FAKE_PAYPAL_ORDER_ID).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void webhook_invalidSignature_returns400() throws Exception {
        when(payPalWebhookVerifier.verify(anyString(), any())).thenReturn(false);

        String webhookBody = buildWebhookEvent("EVT-BAD", "PAYMENT.CAPTURE.COMPLETED", FAKE_PAYPAL_ORDER_ID);

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody))
                .andExpect(status().isBadRequest());

        assertThat(processedEventRepository.count()).isEqualTo(0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private long createOrder() throws Exception {
        String body = """
                {
                  "customerName": "PayPal Test",
                  "customerPhone": "+77001234567",
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

    private static String buildWebhookEvent(String eventId, String eventType, String paypalOrderId) {
        return """
                {
                  "id": "%s",
                  "event_type": "%s",
                  "resource": {
                    "id": "CAP-RESOURCE-001",
                    "status": "COMPLETED",
                    "supplementary_data": {
                      "related_ids": {
                        "order_id": "%s"
                      }
                    }
                  }
                }
                """.formatted(eventId, eventType, paypalOrderId);
    }

    // Webhook without supplementary_data — only has links[rel=up] for order ID extraction
    private static String buildWebhookEventWithLinksFallback(String eventId, String eventType, String paypalOrderId) {
        return """
                {
                  "id": "%s",
                  "event_type": "%s",
                  "resource": {
                    "id": "CAP-FALLBACK-001",
                    "status": "COMPLETED",
                    "links": [
                      {"href": "https://api.paypal.com/v2/payments/captures/CAP-FALLBACK-001",
                       "rel": "self", "method": "GET"},
                      {"href": "https://api.paypal.com/v2/checkout/orders/%s",
                       "rel": "up", "method": "GET"}
                    ]
                  }
                }
                """.formatted(eventId, eventType, paypalOrderId);
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
        group.setName("PayPal Group");
        group.setSlug("paypal-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("PayPal Collection");
        coll.setSlug("paypal-collection");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("PayPal Design");
        design.setSlug("paypal-design");
        design.setStatus(com.nurba.java.enums.DesignStatus.PUBLISHED);
        design.setCreatedAt(LocalDateTime.now());
        design = designRepository.save(design);

        Color color = new Color();
        color.setName("Blue");
        color.setHexCode("#0000FF");
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
        price.setAmount(new BigDecimal("15000.00"));
        designGarmentPriceRepository.save(price);

        Inventory inv = new Inventory();
        inv.setDesignGarment(garment);
        inv.setColor(color);
        inv.setSize(size);
        inv.setQuantity(10);
        inventoryRepository.save(inv);
    }
}
