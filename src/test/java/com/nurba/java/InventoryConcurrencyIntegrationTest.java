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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Concurrency test: two simultaneous orders competing for the last unit of stock.
 * <p>
 * Verifies that {@code SELECT FOR UPDATE} (pessimistic locking) on the Inventory row
 * prevents overselling. Exactly one order must succeed and one must be rejected,
 * leaving inventory at 0 — never at -1.
 */
@SpringBootTest
@ActiveProfiles("test")
class InventoryConcurrencyIntegrationTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

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
        group.setName("Concurrency Group");
        group.setSlug("conc-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("Concurrency Collection");
        coll.setSlug("conc-collection");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("Concurrency Design");
        design.setSlug("conc-design");
        design.setStatus(com.nurba.java.enums.DesignStatus.PUBLISHED);
        design.setCreatedAt(LocalDateTime.now());
        design = designRepository.save(design);

        Color color = new Color();
        color.setName("Gray");
        color.setHexCode("#808080");
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
        price.setAmount(new BigDecimal("7000.00"));
        designGarmentPriceRepository.save(price);
    }

    private void saveInventory(int quantity) {
        DesignGarment garment = designGarmentRepository.findById(garmentId).orElseThrow();
        Color color  = colorRepository.findById(colorId).orElseThrow();
        Size  size   = sizeRepository.findById(sizeId).orElseThrow();

        Inventory inv = new Inventory();
        inv.setDesignGarment(garment);
        inv.setColor(color);
        inv.setSize(size);
        inv.setQuantity(quantity);
        inventoryRepository.save(inv);
    }

    private int currentInventoryQty() {
        return inventoryRepository
                .findByDesignGarment_IdAndColor_IdAndSize_Id(garmentId, colorId, sizeId)
                .map(Inventory::getQuantity)
                .orElseThrow();
    }

    private String orderBody() {
        return """
                {
                  "customerName": "Race Test",
                  "customerPhone": "+77001111111",
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

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Two threads simultaneously POST /api/v1/order, each requesting the last unit.
     * With SELECT FOR UPDATE:
     * - Thread A acquires the row lock, checks qty=1 >= 1, decrements to 0, commits.
     * - Thread B unblocks, reads qty=0, fails the "qty >= requested" check → 400.
     * Final state: exactly 1 order created, inventory = 0, no negative stock.
     */
    @Test
    void twoSimultaneousOrders_onlyOneSucceeds() throws Exception {
        saveInventory(1);   // only 1 unit available

        int threads = 2;
        CountDownLatch startGate = new CountDownLatch(1);   // holds both threads until released
        CountDownLatch doneLatch  = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        String body = orderBody();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();   // both threads start at the same instant
                    int status = mockMvc.perform(post("/api/v1/order")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (status >= 200 && status < 300) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();  // release both threads simultaneously
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(completed).as("Both threads must finish within the timeout").isTrue();
        assertThat(successCount.get()).as("Exactly one order must succeed").isEqualTo(1);
        assertThat(failCount.get()).as("Exactly one order must be rejected").isEqualTo(1);
        assertThat(currentInventoryQty()).as("Inventory must be 0, not negative").isEqualTo(0);
    }
}
