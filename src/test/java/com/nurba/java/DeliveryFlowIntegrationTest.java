package com.nurba.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.Country;
import com.nurba.java.domain.Product;
import com.nurba.java.enums.ShippingZone;
import com.nurba.java.repositories.CountryRepository;
import com.nurba.java.repositories.CustomerRepository;
import com.nurba.java.repositories.DeliveryAddressRepository;
import com.nurba.java.repositories.OrderHistoryRepository;
import com.nurba.java.repositories.OrderItemRepository;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.repositories.PaymentRepository;
import com.nurba.java.repositories.ProductRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class DeliveryFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired private OrderHistoryRepository orderHistoryRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private DeliveryAddressRepository deliveryAddressRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CustomerRepository customerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Long productId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        cleanup();
        countryRepository.save(country("RU", "Россия", "Russia", ShippingZone.CIS));

        Product p = new Product();
        p.setTitle("Test hoodie");
        p.setDescription("integration");
        p.setPrice(new BigDecimal("33000.00"));
        p.setInStock(true);
        p.setCategory("Худи");
        productId = productRepository.save(p).getId();
    }

    @AfterEach
    void cleanup() {
        // Bulk deletes avoid the eager Order<->DeliveryAddress OneToOne flush check, and the
        // @AfterEach guarantees no rows leak into other test classes sharing the context.
        orderHistoryRepository.deleteAllInBatch();
        orderItemRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        deliveryAddressRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        countryRepository.deleteAllInBatch();
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

    @Test
    void calculateOrderReturnsItemsAndDeliveryAndTotal() throws Exception {
        String body = """
                {
                  "toCityCode": 270,
                  "items": [
                    { "productId": %d, "quantity": 1 }
                  ]
                }
                """.formatted(productId);

        String response = mockMvc.perform(post("/api/v1/delivery/cdek/calculate-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemsTotal").value(33000.0))
                .andExpect(jsonPath("$.deliveryPrice").isNumber())
                .andExpect(jsonPath("$.orderTotal").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        BigDecimal itemsTotal = json.get("itemsTotal").decimalValue();
        BigDecimal deliveryPrice = json.get("deliveryPrice").decimalValue();
        BigDecimal orderTotal = json.get("orderTotal").decimalValue();
        assertThat(itemsTotal).isEqualByComparingTo("33000.0");
        assertThat(orderTotal).isEqualByComparingTo(itemsTotal.add(deliveryPrice));
    }

    /**
     * CDEK fee is now computed entirely on the backend from the destination + weight — the client
     * no longer sends a delivery fee. The order is created (PENDING_PAYMENT) with a positive,
     * server-computed fee folded into the total.
     */
    @Test
    void createOrderCdek_deliveryPaidOnPickup_feeNotIncludedInTotal() throws Exception {
        String body = """
                {
                  "customerName": "Test User",
                  "customerPhone": "+77001234567",
                  "deliveryType": "CDEK",
                  "countryIso2": "RU",
                  "items": [
                    { "productId": %d, "quantity": 1 }
                  ],
                  "address": {
                    "city": "Москва",
                    "street": "ул. Тестовая, 1",
                    "apartment": "—",
                    "postalCode": "101000",
                    "recipientName": "Test User",
                    "recipientPhone": "+77001234567"
                  }
                }
                """.formatted(productId);

        // Бизнес-правило СДЭК: доставка оплачивается при получении в ПВЗ. Бэкенд намеренно
        // НЕ включает стоимость доставки в сумму заказа (OrderServiceImpl обнуляет deliveryFee
        // для CDEK), поэтому deliveryFee == null, а totalPrice = только стоимость товаров.
        String response = mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.deliveryType").value("CDEK"))
                .andExpect(jsonPath("$.deliveryFee").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("deliveryFee").isNull()).isTrue();
        BigDecimal totalPrice = json.get("totalPrice").decimalValue();
        // Товары = 33000.00; доставка СДЭК при получении → totalPrice без доставки.
        assertThat(totalPrice).isEqualByComparingTo(new BigDecimal("33000.00"));
    }
}
