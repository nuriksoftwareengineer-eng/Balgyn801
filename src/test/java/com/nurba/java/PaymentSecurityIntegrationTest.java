package com.nurba.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.config.PaymentWebhookProperties;
import com.nurba.java.security.webhook.PaymentRateLimiterFilter;
import com.nurba.java.security.webhook.WebhookSignatureFilter;
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
import com.nurba.java.security.webhook.WebhookSignatureService;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 7 — payment security integration tests.
 *
 * Covers: webhook signature verification, replay protection, idempotent payment init,
 * payment amount validation, and the happy-path order confirmation.
 */
@SpringBootTest
@ActiveProfiles("test")
class PaymentSecurityIntegrationTest {

    private static final String TEST_SECRET      = "test-kaspi-webhook-secret-32chars!!";
    private static final String PROVIDER         = "KASPI";
    private static final String WEBHOOK_URL      = "/api/v1/payments/webhook/" + PROVIDER;
    private static final String INIT_URL         = "/api/v1/payments/init";

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;
    @Autowired private PaymentWebhookProperties webhookProperties;
    @Autowired private PaymentRateLimiterFilter rateLimiterFilter;
    @Autowired private WebhookSignatureFilter signatureFilter;

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
                .addFilters(rateLimiterFilter, signatureFilter)
                .build();
        cleanAll();
        buildFixture();
        // Enable HMAC check for this test class
        webhookProperties.getSecrets().put(PROVIDER, TEST_SECRET);
    }

    @AfterEach
    void tearDown() {
        webhookProperties.getSecrets().remove(PROVIDER);
        cleanAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Signature verification
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void webhook_withValidSignature_returnsOk() throws Exception {
        long orderId   = createOrder(1);
        long paymentId = initPayment(orderId);
        String body    = webhookBody(paymentId, "succeeded");
        String sig     = sign(body, TEST_SECRET);

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WebhookSignatureService.SIGNATURE_HEADER, "hmac_sha256=" + sig)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void webhook_withMissingSignature_returns401() throws Exception {
        long orderId   = createOrder(1);
        long paymentId = initPayment(orderId);
        String body    = webhookBody(paymentId, "succeeded");

        // No signature header → rejected
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_withWrongSignature_returns401() throws Exception {
        long orderId   = createOrder(1);
        long paymentId = initPayment(orderId);
        String body    = webhookBody(paymentId, "succeeded");

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WebhookSignatureService.SIGNATURE_HEADER, "hmac_sha256=deadbeef")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Replay protection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void webhook_replayedEventId_isIdempotent_orderNotDoubleConfirmed() throws Exception {
        long orderId   = createOrder(1);
        long paymentId = initPayment(orderId);
        String body    = webhookBodyWithEvent(paymentId, "succeeded", "evt-001");
        String sig     = sign(body, TEST_SECRET);

        // First call: order → CONFIRMED
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WebhookSignatureService.SIGNATURE_HEADER, "hmac_sha256=" + sig)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        Order confirmedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(confirmedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // Manually try to corrupt: set back to PENDING_PAYMENT to test replay prevention
        confirmedOrder.setStatus(OrderStatus.PENDING_PAYMENT);
        confirmedOrder.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(confirmedOrder);

        // Second call: same event ID → idempotent, no state change
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WebhookSignatureService.SIGNATURE_HEADER, "hmac_sha256=" + sig)
                        .content(body))
                .andExpect(status().isOk());

        // Order must still be PENDING_PAYMENT (replay was ignored, no re-confirmation)
        Order afterReplay = orderRepository.findById(orderId).orElseThrow();
        assertThat(afterReplay.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        // Exactly one processed event record (not two)
        assertThat(processedEventRepository.existsByProviderAndEventId(
                com.nurba.java.enums.PaymentProvider.KASPI, "evt-001")).isTrue();
        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Amount validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void webhook_withWrongAmount_returns400() throws Exception {
        long orderId   = createOrder(1);
        long paymentId = initPayment(orderId);

        // Order total is 12000 KZT; send 1.00 in webhook
        String body = """
                {"paymentId":%d,"status":"succeeded","amount":1.00}
                """.formatted(paymentId).trim();
        String sig  = sign(body, TEST_SECRET);

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WebhookSignatureService.SIGNATURE_HEADER, "hmac_sha256=" + sig)
                        .content(body))
                .andExpect(status().isBadRequest());

        // Payment must NOT have been marked SUCCEEDED
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void webhook_withCorrectAmount_succeeds() throws Exception {
        long orderId   = createOrder(1);
        long paymentId = initPayment(orderId);

        Payment p = paymentRepository.findById(paymentId).orElseThrow();
        String amount = p.getAmount().toPlainString();

        String body = """
                {"paymentId":%d,"status":"succeeded","amount":%s}
                """.formatted(paymentId, amount).trim();
        String sig  = sign(body, TEST_SECRET);

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WebhookSignatureService.SIGNATURE_HEADER, "hmac_sha256=" + sig)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Idempotent payment init
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void initPayment_calledTwice_returnsSamePayment_noDuplicate() throws Exception {
        long orderId = createOrder(1);

        String initBody = """
                {"orderId":%d,"provider":"KASPI"}
                """.formatted(orderId).trim();

        MvcResult r1 = mockMvc.perform(post(INIT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initBody))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult r2 = mockMvc.perform(post(INIT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initBody))
                .andExpect(status().isOk())
                .andReturn();

        long id1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asLong();
        long id2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("id").asLong();

        assertThat(id1).isEqualTo(id2);  // same payment returned
        assertThat(paymentRepository.findAll()).hasSize(1);  // no duplicate rows
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Happy path: order confirmed via webhook
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void webhook_succeeded_confirmsOrder() throws Exception {
        long orderId   = createOrder(1);
        long paymentId = initPayment(orderId);
        String body    = webhookBody(paymentId, "succeeded");
        String sig     = sign(body, TEST_SECRET);

        Order before = orderRepository.findById(orderId).orElseThrow();
        assertThat(before.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WebhookSignatureService.SIGNATURE_HEADER, "hmac_sha256=" + sig)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        Order after = orderRepository.findById(orderId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private long createOrder(int quantity) throws Exception {
        String body = """
                {
                  "customerName": "Security Test",
                  "customerPhone": "+77001234567",
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

    private static String webhookBodyWithEvent(long paymentId, String status, String eventId) {
        return """
                {"paymentId":%d,"status":"%s","eventId":"%s"}
                """.formatted(paymentId, status, eventId).trim();
    }

    private static String sign(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
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
        group.setName("Security Group");
        group.setSlug("sec-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("Security Collection");
        coll.setSlug("sec-collection");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("Security Design");
        design.setSlug("sec-design");
        design.setActive(true);
        design.setCreatedAt(LocalDateTime.now());
        design = designRepository.save(design);

        Color color = new Color();
        color.setName("Red");
        color.setHexCode("#FF0000");
        color = colorRepository.save(color);
        colorId = color.getId();

        Size size = new Size();
        size.setLabel("L");
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
        price.setAmount(new BigDecimal("12000.00"));
        designGarmentPriceRepository.save(price);

        Inventory inv = new Inventory();
        inv.setDesignGarment(garment);
        inv.setColor(color);
        inv.setSize(size);
        inv.setQuantity(10);
        inventoryRepository.save(inv);
    }
}
