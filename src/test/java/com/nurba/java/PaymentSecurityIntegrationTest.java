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
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.GarmentType;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.enums.PaymentProvider;
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
 * Freedom Pay payment security integration tests.
 *
 * Covers: MD5 signature verification on callback, replay protection, amount validation,
 * idempotent payment init, and the happy-path order confirmation.
 *
 * merchantId is blank → stub mode (no real Freedom Pay API call).
 * secretKey is set → signature verification active.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.payment.freedompay.merchant-id=",
        "app.payment.freedompay.secret-key=test-fp-secret-32chars-longer!!",
        "app.payment.freedompay.callback-script-name=freedom-pay",
        "app.security.rate-limit.trust-proxy=true"
})
class PaymentSecurityIntegrationTest {

    private static final String TEST_SECRET   = "test-fp-secret-32chars-longer!!";
    private static final String CALLBACK_URL  = "/api/v1/payments/callback/freedom-pay";
    private static final String INIT_URL      = "/api/v1/payments/init";
    private static final String SCRIPT_NAME   = "freedom-pay";

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;
    @Autowired private PaymentRateLimiterFilter rateLimiterFilter;

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
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Signature verification
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void callback_withValidSignature_returnsOk() throws Exception {
        long orderId = createOrder(1);
        String providerPaymentId = initPaymentAndGetProviderPaymentId(orderId);

        Map<String, String> params = callbackParams(orderId, providerPaymentId, "1", "12000.00");
        params.put("pg_sig", FreedomPaySignature.sign(SCRIPT_NAME, params, TEST_SECRET));

        mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(toMultiValue(params)))
                .andExpect(status().isOk())
                .andExpect(xpath("//pg_status").string("ok"));
    }

    @Test
    void callback_withMissingSignature_returnsRejected() throws Exception {
        long orderId = createOrder(1);
        String providerPaymentId = initPaymentAndGetProviderPaymentId(orderId);

        Map<String, String> params = callbackParams(orderId, providerPaymentId, "1", "12000.00");
        // No pg_sig

        mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(toMultiValue(params)))
                .andExpect(status().isOk())
                .andExpect(xpath("//pg_status").string("rejected"));
    }

    @Test
    void callback_withWrongSignature_returnsRejected() throws Exception {
        long orderId = createOrder(1);
        String providerPaymentId = initPaymentAndGetProviderPaymentId(orderId);

        Map<String, String> params = callbackParams(orderId, providerPaymentId, "1", "12000.00");
        params.put("pg_sig", "deadbeefdeadbeefdeadbeefdeadbeef");

        mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(toMultiValue(params)))
                .andExpect(status().isOk())
                .andExpect(xpath("//pg_status").string("rejected"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Replay protection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void callback_replayedPaymentId_isIdempotent_orderNotDoubleConfirmed() throws Exception {
        long orderId = createOrder(1);
        String providerPaymentId = initPaymentAndGetProviderPaymentId(orderId);

        Map<String, String> params = callbackParams(orderId, providerPaymentId, "1", "12000.00");
        params.put("pg_sig", FreedomPaySignature.sign(SCRIPT_NAME, params, TEST_SECRET));

        // First call: order → CONFIRMED
        mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(toMultiValue(params)))
                .andExpect(status().isOk())
                .andExpect(xpath("//pg_status").string("ok"));

        Order confirmed = orderRepository.findById(orderId).orElseThrow();
        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // Manually reset to PENDING_PAYMENT to verify replay protection
        confirmed.setStatus(OrderStatus.PENDING_PAYMENT);
        confirmed.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(confirmed);

        // Second call: same pg_payment_id → idempotent, no state change
        mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(toMultiValue(params)))
                .andExpect(status().isOk())
                .andExpect(xpath("//pg_status").string("ok"));

        Order afterReplay = orderRepository.findById(orderId).orElseThrow();
        assertThat(afterReplay.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(processedEventRepository.existsByProviderAndEventId(
                PaymentProvider.FREEDOM_PAY, providerPaymentId)).isTrue();
        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Amount validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void callback_withWrongAmount_returnsRejected() throws Exception {
        long orderId = createOrder(1);
        String providerPaymentId = initPaymentAndGetProviderPaymentId(orderId);

        // Order total is 12000 KZT; send 1.00 in callback
        Map<String, String> params = callbackParams(orderId, providerPaymentId, "1", "1.00");
        params.put("pg_sig", FreedomPaySignature.sign(SCRIPT_NAME, params, TEST_SECRET));

        mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(toMultiValue(params)))
                .andExpect(status().isOk())
                .andExpect(xpath("//pg_status").string("rejected"));

        Payment payment = paymentRepository.findByProviderPaymentId(providerPaymentId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void callback_withCorrectAmount_succeeds() throws Exception {
        long orderId = createOrder(1);
        String providerPaymentId = initPaymentAndGetProviderPaymentId(orderId);

        Payment p = paymentRepository.findByProviderPaymentId(providerPaymentId).orElseThrow();
        String amount = p.getAmount().toPlainString();

        Map<String, String> params = callbackParams(orderId, providerPaymentId, "1", amount);
        params.put("pg_sig", FreedomPaySignature.sign(SCRIPT_NAME, params, TEST_SECRET));

        mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(toMultiValue(params)))
                .andExpect(status().isOk())
                .andExpect(xpath("//pg_status").string("ok"));

        Payment updated = paymentRepository.findByProviderPaymentId(providerPaymentId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Idempotent payment init
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void initPayment_calledTwice_returnsSamePayment_noDuplicate() throws Exception {
        long orderId = createOrder(1);
        String body = "{\"orderId\":" + orderId + ",\"provider\":\"FREEDOM_PAY\"}";

        MvcResult r1 = mockMvc.perform(post(INIT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult r2 = mockMvc.perform(post(INIT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        long id1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asLong();
        long id2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("id").asLong();

        assertThat(id1).isEqualTo(id2);
        assertThat(paymentRepository.findAll()).hasSize(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Happy path: order confirmed via callback
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void callback_pgResult1_confirmsOrder() throws Exception {
        long orderId = createOrder(1);
        String providerPaymentId = initPaymentAndGetProviderPaymentId(orderId);

        Order before = orderRepository.findById(orderId).orElseThrow();
        assertThat(before.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        Map<String, String> params = callbackParams(orderId, providerPaymentId, "1", "12000.00");
        params.put("pg_sig", FreedomPaySignature.sign(SCRIPT_NAME, params, TEST_SECRET));

        mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(toMultiValue(params)))
                .andExpect(status().isOk())
                .andExpect(xpath("//pg_status").string("ok"));

        Order after = orderRepository.findById(orderId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void callback_pgResult0_paymentFailed() throws Exception {
        long orderId = createOrder(1);
        String providerPaymentId = initPaymentAndGetProviderPaymentId(orderId);

        Map<String, String> params = callbackParams(orderId, providerPaymentId, "0", "12000.00");
        params.put("pg_sig", FreedomPaySignature.sign(SCRIPT_NAME, params, TEST_SECRET));

        mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(toMultiValue(params)))
                .andExpect(status().isOk())
                .andExpect(xpath("//pg_status").string("ok"));

        Payment payment = paymentRepository.findByProviderPaymentId(providerPaymentId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void callback_pgResult0_onAlreadyConfirmedOrder_orderStaysConfirmed() throws Exception {
        long orderId = createOrder(1);
        String providerPaymentId = initPaymentAndGetProviderPaymentId(orderId);

        // First callback: success → order CONFIRMED
        Map<String, String> successParams = callbackParams(orderId, providerPaymentId, "1", "12000.00");
        successParams.put("pg_sig", FreedomPaySignature.sign(SCRIPT_NAME, successParams, TEST_SECRET));
        mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(toMultiValue(successParams)))
                .andExpect(status().isOk())
                .andExpect(xpath("//pg_status").string("ok"));

        // Second (late/duplicate) callback: failure with different salt to bypass replay check
        Map<String, String> failParams = new LinkedHashMap<>(
                callbackParams(orderId, "fail-" + providerPaymentId, "0", "12000.00"));
        failParams.put("pg_salt", "different-salt-xyz");
        failParams.put("pg_sig", FreedomPaySignature.sign(SCRIPT_NAME, failParams, TEST_SECRET));
        mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(toMultiValue(failParams)))
                .andExpect(status().isOk());

        // Order must remain CONFIRMED despite the late failure callback
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Cancelled order is not revived by late payment
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void callback_pgResult1_onCancelledOrder_orderStaysCancelled() throws Exception {
        long orderId = createOrder(1);
        String providerPaymentId = initPaymentAndGetProviderPaymentId(orderId);

        // Admin cancels the order before payment arrives
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Late FP callback: pg_result=1 (success) arrives after cancellation
        Map<String, String> params = callbackParams(orderId, providerPaymentId, "1", "12000.00");
        params.put("pg_sig", FreedomPaySignature.sign(SCRIPT_NAME, params, TEST_SECRET));

        mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(toMultiValue(params)))
                .andExpect(status().isOk())
                .andExpect(xpath("//pg_status").string("ok"));

        Order after = orderRepository.findById(orderId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void callback_pgResult1_onExpiredOrder_orderStaysExpired() throws Exception {
        long orderId = createOrder(1);
        String providerPaymentId = initPaymentAndGetProviderPaymentId(orderId);

        // Order expired (inventory already released)
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.EXPIRED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        Map<String, String> params = callbackParams(orderId, providerPaymentId, "1", "12000.00");
        params.put("pg_sig", FreedomPaySignature.sign(SCRIPT_NAME, params, TEST_SECRET));

        mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(toMultiValue(params)))
                .andExpect(status().isOk())
                .andExpect(xpath("//pg_status").string("ok"));

        Order after = orderRepository.findById(orderId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(OrderStatus.EXPIRED);
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

    private String initPaymentAndGetProviderPaymentId(long orderId) throws Exception {
        String body = "{\"orderId\":" + orderId + ",\"provider\":\"FREEDOM_PAY\"}";
        MvcResult result = mockMvc.perform(post(INIT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("providerPaymentId").asText();
    }

    private static Map<String, String> callbackParams(long orderId, String providerPaymentId,
                                                       String pgResult, String pgAmount) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("pg_order_id", String.valueOf(orderId));
        params.put("pg_payment_id", providerPaymentId);
        params.put("pg_amount", pgAmount);
        params.put("pg_currency", "KZT");
        params.put("pg_result", pgResult);
        params.put("pg_description", "Test payment");
        params.put("pg_salt", "testsalt123");
        return params;
    }

    private static MultiValueMap<String, String> toMultiValue(Map<String, String> map) {
        MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
        map.forEach(result::add);
        return result;
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
        design.setStatus(com.nurba.java.enums.DesignStatus.PUBLISHED);
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
