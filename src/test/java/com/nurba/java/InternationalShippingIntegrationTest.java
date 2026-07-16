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
import com.nurba.java.domain.GarmentTypeWeight;
import com.nurba.java.domain.IntlZoneTariff;
import com.nurba.java.domain.Inventory;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.Size;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.GarmentType;
import com.nurba.java.enums.IntlShipKind;
import com.nurba.java.enums.ShippingZone;
import com.nurba.java.repositories.GarmentProfileRepository;
import com.nurba.java.repositories.GarmentTypeWeightRepository;
import com.nurba.java.repositories.ExchangeRateRepository;
import com.nurba.java.service.ExchangeRateService;
import com.nurba.java.repositories.CatalogGroupRepository;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.ColorRepository;
import com.nurba.java.repositories.CountryRepository;
import com.nurba.java.repositories.CustomerRepository;
import com.nurba.java.repositories.DeliveryAddressRepository;
import com.nurba.java.repositories.DesignGarmentPriceRepository;
import com.nurba.java.repositories.DesignGarmentRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.repositories.IntlZoneTariffRepository;
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
 * Phase 6 — international shipping (single customer-facing method, AIR internally) and CIS postal.
 * Verifies the fee is computed server-side and the USD amount + exchange rate are snapshotted.
 */
@SpringBootTest
@ActiveProfiles("test")
class InternationalShippingIntegrationTest {

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
    @Autowired private IntlZoneTariffRepository intlZoneTariffRepository;
    @Autowired private GarmentProfileRepository garmentProfileRepository;
    @Autowired private GarmentTypeWeightRepository garmentTypeWeightRepository;
    @Autowired private ExchangeRateRepository exchangeRateRepository;
    @Autowired private ExchangeRateService exchangeRateService;

    @Autowired private OrderHistoryRepository orderHistoryRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private DeliveryAddressRepository deliveryAddressRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CustomerRepository customerRepository;

    private GarmentProfile garmentProfile;
    private Long garmentId;
    private Long colorId;
    private Long sizeId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        cleanAll();
        buildFixture();
        countryRepository.save(country("RU", "Россия", "Russia", ShippingZone.CIS));
        Country us = country("US", "США", "United States", ShippingZone.INTERNATIONAL);
        us.setIntlZone("ZONE_TEST");
        countryRepository.save(us);
        // Zone tariff: страна → зона → цена. Flat, weight-independent.
        intlZoneTariffRepository.save(new IntlZoneTariff(null, "ZONE_TEST", IntlShipKind.AIR, new BigDecimal("6400.00")));

        // Seed deterministic inputs so the fee is independent of any ambient reference data
        // left in the shared in-memory DB by other test classes.
        GarmentTypeWeight w = new GarmentTypeWeight();
        w.setGarmentType(GarmentType.HOODIE);
        w.setWeightKg(new BigDecimal("1.000"));
        garmentTypeWeightRepository.save(w);
        exchangeRateService.setManualRate(new BigDecimal("480.0000"), true);
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
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
        countryRepository.deleteAllInBatch();
        intlZoneTariffRepository.deleteAllInBatch();
        garmentTypeWeightRepository.deleteAll();
        exchangeRateRepository.deleteAll();
        garmentProfileRepository.deleteAll();
    }

    private void buildFixture() {
        CatalogGroup group = new CatalogGroup();
        group.setName("Intl Group");
        group.setSlug("intl-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("Intl Collection");
        coll.setSlug("intl-collection");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("Intl Design");
        design.setSlug("intl-design");
        design.setStatus(com.nurba.java.enums.DesignStatus.PUBLISHED);
        design.setCreatedAt(LocalDateTime.now());
        design = designRepository.save(design);

        GarmentProfile gp = new GarmentProfile();
        gp.setName("Test Profile");
        gp.setWeightKg(new BigDecimal("1.000"));
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
        size.setLabel("L");
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

    private String body(String method, String iso2) {
        return """
                { "customerName": "T", "customerPhone": "+77000000000",
                  "deliveryType": "%s", "countryIso2": "%s", "intlShippingKind": "AIR",
                  "items": [ { "designGarmentId": %d, "colorId": %d, "sizeId": %d, "currency": "KZT", "quantity": 1 } ],
                  "address": { "city": "City", "street": "St 1", "apartment": "—",
                    "postalCode": "00000", "recipientName": "T", "recipientPhone": "+77000000000" } }
                """.formatted(method, iso2, garmentId, colorId, sizeId);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void international_computesFee_andSnapshotsUsdAndRate() throws Exception {
        // Новая модель: страна US → intl_zone ZONE_TEST → тариф AIR 6400 KZT (flat, без веса).
        // Rate 480 → feeUsd = 6400/480 = 13.33.
        MvcResult res = mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON).content(body("INTERNATIONAL", "US")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryFee").value(6400.0))
                .andExpect(jsonPath("$.totalPrice").value(18400.0))
                .andReturn();

        long orderId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getShippingZone()).isEqualTo(ShippingZone.INTERNATIONAL);
        assertThat(order.getTotalWeightKg()).isEqualByComparingTo("1.000");
        assertThat(order.getDeliveryFeeUsd()).isEqualByComparingTo("13.33");
        assertThat(order.getExchangeRateKztUsd()).isEqualByComparingTo("480.0000");
    }

    @Test
    void cisPostal_rejectedForCis() throws Exception {
        // Zone matrix change: CIS supports CDEK only. POSTAL is Kazakhstan-domestic only.
        mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON).content(body("POSTAL", "RU")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Способ доставки недоступен для выбранной страны"));
    }

    @Test
    void cdek_rejectedForInternationalCountry() throws Exception {
        mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON).content(body("CDEK", "US")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Способ доставки недоступен для выбранной страны"));
    }
}
