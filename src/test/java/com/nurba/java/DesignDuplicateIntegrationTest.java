package com.nurba.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Color;
import com.nurba.java.domain.Design;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.DesignGarmentPrice;
import com.nurba.java.domain.GarmentProfile;
import com.nurba.java.domain.Inventory;
import com.nurba.java.domain.Size;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.DesignStatus;
import com.nurba.java.repositories.CatalogGroupRepository;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.ColorRepository;
import com.nurba.java.repositories.DesignGarmentPriceRepository;
import com.nurba.java.repositories.DesignGarmentRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.repositories.GarmentProfileRepository;
import com.nurba.java.repositories.InventoryRepository;
import com.nurba.java.repositories.SizeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies POST /api/v1/admin/catalog/designs/{id}/duplicate:
 *   - garments, colors, sizes and prices are copied to a new design
 *   - inventory is NOT copied
 *   - status/publishedAt/archivedAt/viewCount are reset, not copied from the source
 *   - name and slug collisions are resolved with an incrementing suffix
 *   - duplicating a non-existent design returns 404
 */
@SpringBootTest
@ActiveProfiles("test")
class DesignDuplicateIntegrationTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;
    @Autowired private CatalogGroupRepository groupRepository;
    @Autowired private CollectionRepository collectionRepository;
    @Autowired private DesignRepository designRepository;
    @Autowired private DesignGarmentRepository garmentRepository;
    @Autowired private DesignGarmentPriceRepository priceRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private GarmentProfileRepository garmentProfileRepository;

    private Long collectionId;
    private Long colorId;
    private Long sizeId;
    private GarmentProfile garmentProfile;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        cleanAll();
        buildBaseFixture();
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
        inventoryRepository.deleteAll();
        priceRepository.deleteAll();
        garmentRepository.deleteAll();
        designRepository.deleteAll();
        collectionRepository.deleteAll();
        groupRepository.deleteAll();
        colorRepository.deleteAll();
        sizeRepository.deleteAll();
        garmentProfileRepository.deleteAll();
    }

    private void buildBaseFixture() {
        CatalogGroup group = new CatalogGroup();
        group.setName("Dup Test Group");
        group.setSlug("dup-test-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = groupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("Dup Test Collection");
        coll.setSlug("dup-test-collection");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);
        collectionId = coll.getId();

        Color color = new Color();
        color.setName("Black");
        color.setHexCode("#000000");
        color = colorRepository.save(color);
        colorId = color.getId();

        Size size = new Size();
        size.setLabel("M");
        size = sizeRepository.save(size);
        sizeId = size.getId();

        GarmentProfile gp = new GarmentProfile();
        gp.setName("Dup Test Profile");
        gp.setWeightKg(new BigDecimal("0.400"));
        gp.setLengthCm(30);
        gp.setWidthCm(25);
        gp.setHeightCm(6);
        gp.setSortOrder(0);
        garmentProfile = garmentProfileRepository.save(gp);
    }

    /** A published-looking design (image + one fully-configured garment + inventory). */
    private Design createSourceDesignWithGarmentAndInventory(String slug, String name) {
        Collection coll = collectionRepository.findById(collectionId).orElseThrow();
        Design d = new Design();
        d.setCollection(coll);
        d.setName(name);
        d.setSlug(slug);
        d.setDescription("Source description");
        d.setMainImageUrl("https://cdn.example.com/" + slug + ".jpg");
        d.setStatus(DesignStatus.PUBLISHED);
        d.setPublishedAt(LocalDateTime.now());
        d.setViewCount(42);
        d.setCreatedAt(LocalDateTime.now());
        d = designRepository.save(d);

        Color color = colorRepository.findById(colorId).orElseThrow();
        Size size = sizeRepository.findById(sizeId).orElseThrow();

        DesignGarment g = new DesignGarment();
        g.setDesign(d);
        g.setGarmentProfile(garmentProfile);
        g.setActive(true);
        g.getColors().add(color);
        g.getSizes().add(size);
        g = garmentRepository.save(g);

        DesignGarmentPrice price = new DesignGarmentPrice();
        price.setDesignGarment(g);
        price.setCurrency(Currency.KZT);
        price.setAmount(new BigDecimal("25000.00"));
        priceRepository.save(price);

        Inventory inv = new Inventory();
        inv.setDesignGarment(g);
        inv.setColor(color);
        inv.setSize(size);
        inv.setQuantity(15);
        inventoryRepository.save(inv);

        return d;
    }

    // @Transactional here only to keep a Hibernate session open long enough for the assertions
    // below to read the copy's lazy colors/sizes collections after the MockMvc call returns —
    // duplicate() itself already reads them inside its own transaction, same as any other write.
    @Test
    @Transactional
    void duplicate_copiesGarmentsColorsAndPrices_butNotInventory_andResetsLifecycleFields() throws Exception {
        Design source = createSourceDesignWithGarmentAndInventory("dup-source-1", "Худи");

        String body = mockMvc.perform(post("/api/v1/admin/catalog/designs/" + source.getId() + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Худи (копия)"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.activeGarmentCount").value(1))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        long copyId = json.get("id").asLong();
        assertThat(copyId).isNotEqualTo(source.getId());
        assertThat(json.get("slug").asText()).isEqualTo("dup-source-1-copy");
        assertThat(json.get("mainImageUrl").asText()).isEqualTo(source.getMainImageUrl());
        assertThat(json.get("description").asText()).isEqualTo("Source description");

        Design copy = designRepository.findById(copyId).orElseThrow();
        assertThat(copy.getStatus()).isEqualTo(DesignStatus.DRAFT);
        assertThat(copy.getPublishedAt()).isNull();
        assertThat(copy.getArchivedAt()).isNull();
        assertThat(copy.getViewCount()).isEqualTo(0);
        assertThat(copy.getSortOrder()).isNull();

        List<DesignGarment> copyGarments = garmentRepository.findByDesign_Id(copyId);
        assertThat(copyGarments).hasSize(1);
        DesignGarment copyGarment = copyGarments.get(0);
        assertThat(copyGarment.getGarmentProfile().getId()).isEqualTo(garmentProfile.getId());
        assertThat(copyGarment.getColors()).extracting("id").containsExactly(colorId);
        assertThat(copyGarment.getSizes()).extracting("id").containsExactly(sizeId);

        List<DesignGarmentPrice> copyPrices = priceRepository.findByDesignGarment_Id(copyGarment.getId());
        assertThat(copyPrices).hasSize(1);
        assertThat(copyPrices.get(0).getCurrency()).isEqualTo(Currency.KZT);
        assertThat(copyPrices.get(0).getAmount()).isEqualByComparingTo("25000.00");

        // The whole point of this guard: inventory must NOT follow the copy.
        assertThat(inventoryRepository.findByDesignGarment_Id(copyGarment.getId())).isEmpty();

        // Source itself must be completely untouched.
        Design reloadedSource = designRepository.findById(source.getId()).orElseThrow();
        assertThat(reloadedSource.getStatus()).isEqualTo(DesignStatus.PUBLISHED);
        assertThat(reloadedSource.getViewCount()).isEqualTo(42);
        assertThat(garmentRepository.findByDesign_Id(source.getId())).hasSize(1);
    }

    @Test
    void duplicate_calledTwice_incrementsNameAndSlugSuffix() throws Exception {
        Design source = createSourceDesignWithGarmentAndInventory("dup-source-2", "Свитшот");

        mockMvc.perform(post("/api/v1/admin/catalog/designs/" + source.getId() + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Свитшот (копия)"))
                .andExpect(jsonPath("$.slug").value("dup-source-2-copy"));

        mockMvc.perform(post("/api/v1/admin/catalog/designs/" + source.getId() + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Свитшот (копия 2)"))
                .andExpect(jsonPath("$.slug").value("dup-source-2-copy-2"));

        mockMvc.perform(post("/api/v1/admin/catalog/designs/" + source.getId() + "/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Свитшот (копия 3)"))
                .andExpect(jsonPath("$.slug").value("dup-source-2-copy-3"));
    }

    @Test
    void duplicate_nonExistentDesign_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/admin/catalog/designs/999999/duplicate")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
}
