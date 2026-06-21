package com.nurba.java;

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
import com.nurba.java.repositories.AppUserRepository;
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
import com.nurba.java.repositories.SizeRepository;
import com.nurba.java.security.SensitiveEndpointRateLimiterFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Анти-спам rate limit на POST /api/v1/order (тот же механизм, что login/register/custom-design).
 * Лимит занижен до 3/мин через @TestPropertySource, чтобы детерминированно проверить 429.
 * НЕ меняет бизнес-логику заказа: проверяется только ограничитель перед контроллером.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.rate-limit.order-per-minute=3",
        "app.security.rate-limit.trust-proxy=true"
})
class OrderRateLimitIntegrationTest {

    private static final String ORDER_URL = "/api/v1/order";

    @Autowired private WebApplicationContext context;
    @Autowired private SensitiveEndpointRateLimiterFilter rateLimiterFilter;
    private MockMvc mockMvc;

    @Autowired private CatalogGroupRepository catalogGroupRepository;
    @Autowired private CollectionRepository collectionRepository;
    @Autowired private DesignRepository designRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private DesignGarmentRepository designGarmentRepository;
    @Autowired private DesignGarmentPriceRepository designGarmentPriceRepository;
    @Autowired private InventoryRepository inventoryRepository;

    @Autowired private OrderHistoryRepository orderHistoryRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AppUserRepository appUserRepository;

    private Long garmentId;
    private Long colorId;
    private Long sizeId;

    @BeforeEach
    void setUp() {
        // Применяем только сам rate-limiter (заказ — public endpoint, Spring Security не нужен).
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(rateLimiterFilter)
                .build();
        cleanAll();
        buildFixture();
    }

    @Test
    void orderCreation_overLimit_returns429_andLimitIsPerIp() throws Exception {
        String body = orderBody();
        String ip = "203.0.113.77";

        // Лимит = 3: первые три заказа с одного IP проходят.
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post(ORDER_URL)
                            .header("X-Forwarded-For", ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        // Четвёртый с того же IP — превышение лимита → 429 (до контроллера, без создания заказа).
        mockMvc.perform(post(ORDER_URL)
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests());

        // Ограничение по IP: запрос с другого IP не заблокирован.
        mockMvc.perform(post(ORDER_URL)
                        .header("X-Forwarded-For", "198.51.100.9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    // ── Fixture ────────────────────────────────────────────────────────────────

    private String orderBody() {
        return """
                {
                  "customerName": "RateLimit Test",
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
    }

    private void cleanAll() {
        orderHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();
        appUserRepository.deleteAll();
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
        group.setName("RateLimit Group");
        group.setSlug("ratelimit-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("RateLimit Collection");
        coll.setSlug("ratelimit-coll");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("RateLimit Design");
        design.setSlug("ratelimit-design");
        design.setStatus(com.nurba.java.enums.DesignStatus.PUBLISHED);
        design.setCreatedAt(LocalDateTime.now());
        design = designRepository.save(design);

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
        garment.setGarmentType(GarmentType.HOODIE);
        garment.setActive(true);
        garment.getColors().add(color);
        garment.getSizes().add(size);
        garment = designGarmentRepository.save(garment);
        garmentId = garment.getId();

        DesignGarmentPrice price = new DesignGarmentPrice();
        price.setDesignGarment(garment);
        price.setCurrency(Currency.KZT);
        price.setAmount(new BigDecimal("5000.00"));
        designGarmentPriceRepository.save(price);

        // Достаточно стока на все успешные заказы (3 с одного IP + 1 с другого).
        Inventory inv = new Inventory();
        inv.setDesignGarment(garment);
        inv.setColor(color);
        inv.setSize(size);
        inv.setQuantity(50);
        inventoryRepository.save(inv);
    }
}
