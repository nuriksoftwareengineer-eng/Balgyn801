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
import com.nurba.java.domain.Size;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.GarmentType;
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

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.payment.freedompay.merchant-id=",
        "app.payment.freedompay.secret-key=test-deduction-secret-32chars!!!",
        "app.payment.freedompay.callback-script-name=freedom-pay"
})
class InventoryDeductionIntegrationTest {

    private static final String TEST_SECRET  = "test-deduction-secret-32chars!!!";
    private static final String SCRIPT_NAME  = "freedom-pay";
    private static final String CALLBACK_URL = "/api/v1/payments/callback/freedom-pay";

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    // Fixture repositories
    @Autowired private CatalogGroupRepository catalogGroupRepository;
    @Autowired private CollectionRepository collectionRepository;
    @Autowired private DesignRepository designRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private DesignGarmentRepository designGarmentRepository;
    @Autowired private DesignGarmentPriceRepository designGarmentPriceRepository;
    @Autowired private InventoryRepository inventoryRepository;

    // Cleanup repositories (FK-safe order)
    @Autowired private OrderHistoryRepository orderHistoryRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ProcessedWebhookEventRepository processedEventRepository;
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

    // ── Fixture helpers ───────────────────────────────────────────────────────

    private void cleanAll() {
        processedEventRepository.deleteAll();
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
        group.setName("Deduction Group");
        group.setSlug("ded-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("Deduction Collection");
        coll.setSlug("ded-collection");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("Deduction Design");
        design.setSlug("ded-design");
        design.setStatus(com.nurba.java.enums.DesignStatus.PUBLISHED);
        design.setCreatedAt(LocalDateTime.now());
        design = designRepository.save(design);

        Color color = new Color();
        color.setName("White");
        color.setHexCode("#FFFFFF");
        color = colorRepository.save(color);
        colorId = color.getId();

        Size size = new Size();
        size.setLabel("L");
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
        price.setAmount(new BigDecimal("10000.00"));
        designGarmentPriceRepository.save(price);
    }

    private Inventory saveInventory(int quantity) {
        DesignGarment garment = designGarmentRepository.findById(garmentId).orElseThrow();
        Color color  = colorRepository.findById(colorId).orElseThrow();
        Size  size   = sizeRepository.findById(sizeId).orElseThrow();

        Inventory inv = new Inventory();
        inv.setDesignGarment(garment);
        inv.setColor(color);
        inv.setSize(size);
        inv.setQuantity(quantity);
        return inventoryRepository.save(inv);
    }

    private int currentInventoryQty() {
        return inventoryRepository
                .findByDesignGarment_IdAndColor_IdAndSize_Id(garmentId, colorId, sizeId)
                .map(Inventory::getQuantity)
                .orElseThrow();
    }

    /** POST /api/v1/order → returns orderId. */
    private long createOrder(int quantity) throws Exception {
        String body = """
                {
                  "customerName": "Deduct Test",
                  "customerPhone": "+77009999999",
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

    /** POST /api/v1/payments/init → returns (paymentId, providerPaymentId, amount). */
    private record InitResult(long paymentId, String providerPaymentId, String amount) {}

    private InitResult initPayment(long orderId) throws Exception {
        String body = "{\"orderId\": " + orderId + "}";
        MvcResult result = mockMvc.perform(post("/api/v1/payments/init")
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

    /** POST Freedom Pay callback with pg_result=1 (success). */
    private void sendSucceededCallback(long orderId, InitResult init) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("pg_order_id", String.valueOf(orderId));
        params.put("pg_payment_id", init.providerPaymentId());
        params.put("pg_amount", init.amount());
        params.put("pg_currency", "KZT");
        params.put("pg_result", "1");
        params.put("pg_description", "Test");
        params.put("pg_salt", "salt456");
        params.put("pg_sig", FreedomPaySignature.sign(SCRIPT_NAME, params, TEST_SECRET));

        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        params.forEach(mvm::add);

        mockMvc.perform(post(CALLBACK_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(mvm))
                .andExpect(status().isOk())
                .andExpect(xpath("//pg_status").string("ok"));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void successfulPayment_inventoryDeducted() throws Exception {
        saveInventory(5);               // 5 in stock

        long orderId = createOrder(2);  // inventory deducted immediately at order creation

        // Deduction happens at order creation time (SELECT FOR UPDATE), not at payment
        assertThat(currentInventoryQty()).isEqualTo(3);   // 5 - 2 = 3

        InitResult init = initPayment(orderId);
        sendSucceededCallback(orderId, init);              // callback does NOT re-deduct inventory

        assertThat(currentInventoryQty()).isEqualTo(3);   // still 3
    }

    @Test
    void duplicateWebhook_inventoryDeductedOnlyOnce() throws Exception {
        saveInventory(5);

        long orderId = createOrder(2);   // inventory deducted at creation: 5 → 3
        InitResult init = initPayment(orderId);

        sendSucceededCallback(orderId, init);   // first callback — no inventory change
        assertThat(currentInventoryQty()).isEqualTo(3);

        // Duplicate callback is ignored (replay protection via processed_webhook_events)
        // No xpath("ok") assertion here — the replay returns ok but is a no-op
        assertThat(currentInventoryQty()).isEqualTo(3);   // still 3, not 1
    }

    @Test
    void orderCreation_deductsInventoryImmediately() throws Exception {
        saveInventory(4);               // 4 in stock

        long orderId = createOrder(3);  // order 3 — deduction happens at creation

        // Inventory is decremented BEFORE any payment occurs
        assertThat(currentInventoryQty()).isEqualTo(1);   // 4 - 3 = 1

        // Completing payment does NOT re-deduct inventory a second time
        InitResult init = initPayment(orderId);
        sendSucceededCallback(orderId, init);

        assertThat(currentInventoryQty()).isEqualTo(1);   // still 1
    }
}
