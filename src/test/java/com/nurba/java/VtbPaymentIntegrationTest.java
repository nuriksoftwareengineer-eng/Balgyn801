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
import com.nurba.java.domain.GarmentProfile;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;
import com.nurba.java.payment.vtb.VtbHttpClient;
import com.nurba.java.payment.vtb.dto.VtbOrderStatusResponse;
import com.nurba.java.payment.vtb.dto.VtbRegisterResponse;
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
import com.nurba.java.repositories.GarmentProfileRepository;
import com.nurba.java.repositories.ProcessedWebhookEventRepository;
import com.nurba.java.repositories.SizeRepository;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the VTB Kazakhstan payment flow.
 *
 * VtbHttpClient is mocked — no real VTB API calls.
 * Covers: init, idempotency, callback (deposited/declined), verifyReturn,
 * duplicate callback (replay protection), and cancelled-order guard.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.payment.vtb.username=test-vtb-user",
        "app.payment.vtb.password=test-vtb-pass",
        "app.payment.vtb.sandbox=true",
        "app.payment.vtb.supported-currencies=398",
        "app.payment.vtb.fallback-to-kzt=false",
        "app.payment.freedompay.merchant-id=",
        "app.payment.freedompay.secret-key=test-vtb-fp-secret-32chars!!!"
})
class VtbPaymentIntegrationTest {

    private static final String INIT_URL          = "/api/v1/payments/init";
    private static final String CALLBACK_URL      = "/api/v1/payments/callback/vtb-kz";
    private static final String VERIFY_RETURN_URL = "/api/v1/payments/vtb-kz/verify-return";

    private static final String FAKE_VTB_ORDER_ID = "vtb-order-uuid-001";
    private static final String FAKE_FORM_URL     = "https://ecom.vtb.kz/payment/merchants/test/payment_ru.html?mdOrder=" + FAKE_VTB_ORDER_ID;

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private VtbHttpClient vtbHttpClient;

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
    @Autowired private GarmentProfileRepository garmentProfileRepository;

    private GarmentProfile garmentProfile;
    private Long garmentId;
    private Long colorId;
    private Long sizeId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        cleanAll();
        buildFixture();

        // Default mock: register.do returns a valid VTB order
        when(vtbHttpClient.register(any()))
                .thenReturn(new VtbRegisterResponse(FAKE_VTB_ORDER_ID, FAKE_FORM_URL, 0, null));

        // Default mock: getOrderStatus returns DEPOSITED (orderStatus=2) — amount 1000000 tiyn = 10000.00 KZT
        when(vtbHttpClient.getOrderStatus(anyString()))
                .thenReturn(new VtbOrderStatusResponse(2, 0, null, 1000000L, 398, null));
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Init — returns PENDING with redirect URL
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void initPayment_vtb_returnsPendingWithRedirectUrl() throws Exception {
        long orderId = createOrder();

        mockMvc.perform(post(INIT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"provider\":\"VTB_KZ\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("VTB_KZ"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentUrl").value(FAKE_FORM_URL))
                .andExpect(jsonPath("$.providerPaymentId").value(FAKE_VTB_ORDER_ID));

        assertThat(paymentRepository.findAll()).hasSize(1);
        Payment p = paymentRepository.findAll().get(0);
        assertThat(p.getProvider()).isEqualTo(PaymentProvider.VTB_KZ);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(p.getCurrency()).isEqualTo("KZT");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Init — idempotency: second call returns the same payment row
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void initPayment_vtb_idempotent_returnsSamePayment() throws Exception {
        long orderId = createOrder();
        String body = "{\"orderId\":" + orderId + ",\"provider\":\"VTB_KZ\"}";

        MvcResult r1 = mockMvc.perform(post(INIT_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        MvcResult r2 = mockMvc.perform(post(INIT_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();

        long id1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asLong();
        long id2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("id").asLong();
        assertThat(id1).isEqualTo(id2);
        assertThat(paymentRepository.findAll()).hasSize(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Callback — DEPOSITED confirms payment and order
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void callback_deposited_confirmsPaymentAndOrder() throws Exception {
        long orderId = createOrder();
        initVtbPayment(orderId);

        // VTB sends GET callback with mdOrder and operation
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("mdOrder", FAKE_VTB_ORDER_ID);
        params.add("operation", "deposited");
        params.add("status", "1");

        mockMvc.perform(get(CALLBACK_URL).params(params))
                .andExpect(status().isOk());

        Payment payment = paymentRepository.findByProviderPaymentId(FAKE_VTB_ORDER_ID).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        assertThat(processedEventRepository
                .existsByProviderAndEventId(PaymentProvider.VTB_KZ, FAKE_VTB_ORDER_ID)).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Callback — DECLINED marks payment FAILED
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void callback_declined_failsPayment() throws Exception {
        long orderId = createOrder();
        initVtbPayment(orderId);

        // Override: VTB reports DECLINED (orderStatus=3)
        when(vtbHttpClient.getOrderStatus(anyString()))
                .thenReturn(new VtbOrderStatusResponse(3, 0, null, 0L, 398, null));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("mdOrder", FAKE_VTB_ORDER_ID);
        params.add("operation", "declined");
        params.add("status", "0");

        mockMvc.perform(get(CALLBACK_URL).params(params))
                .andExpect(status().isOk());

        Payment payment = paymentRepository.findByProviderPaymentId(FAKE_VTB_ORDER_ID).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. verifyReturn — DEPOSITED confirms payment via return URL
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void verifyReturn_deposited_confirmsPayment() throws Exception {
        long orderId = createOrder();
        initVtbPayment(orderId);

        String body = "{\"orderId\":\"" + FAKE_VTB_ORDER_ID + "\"}";

        mockMvc.perform(post(VERIFY_RETURN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        Payment payment = paymentRepository.findByProviderPaymentId(FAKE_VTB_ORDER_ID).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Callback duplicate — replay protection via ProcessedWebhookEvent
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void callback_duplicate_isIdempotent() throws Exception {
        long orderId = createOrder();
        initVtbPayment(orderId);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("mdOrder", FAKE_VTB_ORDER_ID);
        params.add("operation", "deposited");

        // First callback
        mockMvc.perform(get(CALLBACK_URL).params(params)).andExpect(status().isOk());
        // Duplicate
        mockMvc.perform(get(CALLBACK_URL).params(params)).andExpect(status().isOk());

        // Exactly one ProcessedWebhookEvent
        assertThat(processedEventRepository.count()).isEqualTo(1);
        // Order confirmed exactly once
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. verifyReturn duplicate — callback already confirmed, verifyReturn is a no-op
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void verifyReturn_afterCallback_isIdempotent() throws Exception {
        long orderId = createOrder();
        initVtbPayment(orderId);

        // Confirm via callback first
        MultiValueMap<String, String> callbackParams = new LinkedMultiValueMap<>();
        callbackParams.add("mdOrder", FAKE_VTB_ORDER_ID);
        callbackParams.add("operation", "deposited");
        mockMvc.perform(get(CALLBACK_URL).params(callbackParams)).andExpect(status().isOk());

        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);

        // verifyReturn arrives late — payment already SUCCEEDED, must be idempotent
        String body = "{\"orderId\":\"" + FAKE_VTB_ORDER_ID + "\"}";
        mockMvc.perform(post(VERIFY_RETURN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        // Order still CONFIRMED, no duplicate events
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. Init on cancelled order → 400
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void initPayment_vtb_cancelledOrder_returns400() throws Exception {
        long orderId = createOrder();
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        mockMvc.perform(post(INIT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"provider\":\"VTB_KZ\"}"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private long createOrder() throws Exception {
        String body = """
                {
                  "customerName": "VTB Test",
                  "customerPhone": "+77001112233",
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

    private void initVtbPayment(long orderId) throws Exception {
        mockMvc.perform(post(INIT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"provider\":\"VTB_KZ\"}"))
                .andExpect(status().isOk());
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
        garmentProfileRepository.deleteAll();
    }

    private void buildFixture() {
        CatalogGroup group = new CatalogGroup();
        group.setName("VTB Group");
        group.setSlug("vtb-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("VTB Collection");
        coll.setSlug("vtb-collection");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("VTB Design");
        design.setSlug("vtb-design");
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
        color.setName("Red");
        color.setHexCode("#FF0000");
        color = colorRepository.save(color);
        colorId = color.getId();

        Size size = new Size();
        size.setLabel("XL");
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
        price.setAmount(new BigDecimal("10000.00"));
        designGarmentPriceRepository.save(price);

        Inventory inv = new Inventory();
        inv.setDesignGarment(garment);
        inv.setColor(color);
        inv.setSize(size);
        inv.setQuantity(5);
        inventoryRepository.save(inv);
    }
}
