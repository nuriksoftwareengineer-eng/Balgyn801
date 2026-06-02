package com.nurba.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.Product;
import com.nurba.java.repositories.ProductRepository;
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
    private ObjectMapper objectMapper;

    private Long productId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        productRepository.deleteAll();
        Product p = new Product();
        p.setTitle("Test hoodie");
        p.setDescription("integration");
        p.setPrice(new BigDecimal("33000.00"));
        p.setInStock(true);
        p.setCategory("Худи");
        productId = productRepository.save(p).getId();
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

    @Test
    void createOrderCdekWithoutDeliveryFeeReturnsBadRequest() throws Exception {
        String body = """
                {
                  "customerName": "Test User",
                  "customerPhone": "+77001234567",
                  "deliveryType": "CDEK",
                  "items": [
                    { "productId": %d, "quantity": 1 }
                  ],
                  "address": {
                    "city": "Алматы",
                    "street": "СДЭК ПВЗ \\"Smoke\\" [STUB-270-1]: ул. Тестовая, 1",
                    "apartment": "—",
                    "postalCode": "050000",
                    "recipientName": "Test User",
                    "recipientPhone": "+77001234567"
                  }
                }
                """.formatted(productId);

        mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Для доставки СДЭК сначала рассчитайте стоимость на сайте"));
    }
}
