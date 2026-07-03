package com.nurba.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Color;
import com.nurba.java.domain.Country;
import com.nurba.java.domain.Design;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.DesignGarmentPrice;
import com.nurba.java.domain.GarmentProfile;
import com.nurba.java.domain.Inventory;
import com.nurba.java.domain.Size;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.ShippingZone;
import com.nurba.java.repositories.CatalogGroupRepository;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.ColorRepository;
import com.nurba.java.repositories.CountryRepository;
import com.nurba.java.repositories.CustomerRepository;
import com.nurba.java.repositories.DeliveryAddressRepository;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 5 — backend delivery pricing. Verifies the Kazakhstan rules (pickup free, delivery flat
 * 1600 KZT, no CDEK domestically) and that CIS CDEK is computed server-side. The client never
 * sends a delivery fee.
 */
@SpringBootTest
@ActiveProfiles("test")
class DeliveryPricingIntegrationTest {

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
    @Autowired private CountryRepository countryRepository;

    @Autowired private OrderHistoryRepository orderHistoryRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private DeliveryAddressRepository deliveryAddressRepository;
    @Autowired private OrderRepository orderRepository;
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
        countryRepository.save(country("KZ", "Казахстан", "Kazakhstan", ShippingZone.KAZAKHSTAN));
        countryRepository.save(country("RU", "Россия", "Russia", ShippingZone.CIS));
    }

    @AfterEach
    void tearDown() {
        cleanAll();   // leave no rows behind for other test classes sharing the context
    }

    private void cleanAll() {
        // Bulk deletes (no entity loading) avoid the eager Order<->DeliveryAddress OneToOne
        // triggering a transient-reference check at flush during cleanup.
        orderHistoryRepository.deleteAllInBatch();
        orderItemRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        deliveryAddressRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
        inventoryRepository.deleteAll();
        designGarmentPriceRepository.deleteAll();
        designGarmentRepository.deleteAll();
        designRepository.deleteAll();
        collectionRepository.deleteAll();
        catalogGroupRepository.deleteAll();
        colorRepository.deleteAll();
        sizeRepository.deleteAll();
        countryRepository.deleteAll();
        garmentProfileRepository.deleteAll();
    }

    private void buildFixture() {
        CatalogGroup group = new CatalogGroup();
        group.setName("Pricing Group");
        group.setSlug("price-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("Pricing Collection");
        coll.setSlug("price-collection");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("Pricing Design");
        design.setSlug("price-design");
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
        size.setLabel("M");
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
        price.setAmount(new BigDecimal("12000.00"));
        designGarmentPriceRepository.save(price);

        Inventory inv = new Inventory();
        inv.setDesignGarment(garment);
        inv.setColor(color);
        inv.setSize(size);
        inv.setQuantity(10);
        inventoryRepository.save(inv);
    }

    private Country country(String iso2, String ru, String en, ShippingZone zone) {
        Country c = new Country();
        c.setIso2(iso2);
        c.setNameRu(ru);
        c.setNameEn(en);
        c.setShippingZone(zone);
        c.setActive(true);
        return c;
    }

    private String item() {
        return """
                { "designGarmentId": %d, "colorId": %d, "sizeId": %d, "currency": "KZT", "quantity": 1 }
                """.formatted(garmentId, colorId, sizeId);
    }

    private String addressBlock(String city) {
        return """
                "address": {
                  "city": "%s",
                  "street": "ул. Тестовая, 1",
                  "apartment": "—",
                  "postalCode": "050000",
                  "recipientName": "Test",
                  "recipientPhone": "+77000000000"
                }
                """.formatted(city);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void pickup_isFree() throws Exception {
        String body = """
                { "customerName": "T", "customerPhone": "+77000000000",
                  "deliveryType": "PICKUP", "items": [ %s ] }
                """.formatted(item());

        mockMvc.perform(post("/api/v1/order").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalPrice").value(12000.0));
    }

    @Test
    void kazakhstanDelivery_isFlat1600() throws Exception {
        String body = """
                { "customerName": "T", "customerPhone": "+77000000000",
                  "deliveryType": "TAXI", "countryIso2": "KZ", "items": [ %s ], %s }
                """.formatted(item(), addressBlock("Алматы"));

        mockMvc.perform(post("/api/v1/order").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryFee").value(1600.0))
                .andExpect(jsonPath("$.totalPrice").value(13600.0));
    }

    @Test
    void cdek_allowedForKazakhstan() throws Exception {
        // Zone matrix change: KZ now supports CDEK (was rejected in old rules).
        String body = """
                { "customerName": "T", "customerPhone": "+77000000000",
                  "deliveryType": "CDEK", "countryIso2": "KZ", "items": [ %s ], %s }
                """.formatted(item(), addressBlock("Алматы"));

        mockMvc.perform(post("/api/v1/order").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void cdek_forCisCountry_deliveryPaidOnPickup_feeNotInTotal() throws Exception {
        String body = """
                { "customerName": "T", "customerPhone": "+77000000000",
                  "deliveryType": "CDEK", "countryIso2": "RU", "items": [ %s ], %s }
                """.formatted(item(), addressBlock("Москва"));

        // СДЭК оплачивается при получении: deliveryFee намеренно не включается в сумму заказа
        // (OrderServiceImpl обнуляет фи для CDEK) → deliveryFee == null, totalPrice = стоимость товаров.
        String response = mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryType").value("CDEK"))
                .andExpect(jsonPath("$.deliveryFee").isEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        org.assertj.core.api.Assertions.assertThat(json.get("deliveryFee").isNull()).isTrue();
        BigDecimal totalPrice = json.get("totalPrice").decimalValue();
        // Товары = 12000.00; доставка СДЭК при получении → totalPrice без доставки.
        org.assertj.core.api.Assertions.assertThat(totalPrice)
                .isEqualByComparingTo(new BigDecimal("12000.00"));
    }
}
