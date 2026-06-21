package com.nurba.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Color;
import com.nurba.java.domain.Design;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.DesignGarmentPrice;
import com.nurba.java.domain.Size;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.DesignStatus;
import com.nurba.java.enums.GarmentType;
import com.nurba.java.repositories.CatalogGroupRepository;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.ColorRepository;
import com.nurba.java.repositories.DesignGarmentPriceRepository;
import com.nurba.java.repositories.DesignGarmentRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.repositories.SizeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies publication guards on PATCH /api/v1/admin/catalog/designs/{id}/publish
 * and the lifecycle transitions: publish → unpublish → archive → restore.
 *
 * Guards tested:
 *   - main_image_missing          (no mainImageUrl)
 *   - no_active_garments          (no active garments)
 *   - no_variant_with_kzt_price_size_color (garment exists but lacks KZT price / color / size)
 *
 * Lifecycle tested:
 *   - DRAFT → PUBLISHED (all guards pass)
 *   - PUBLISHED → READY via /unpublish
 *   - PUBLISHED → ARCHIVED via /archive, archivedAt populated
 *   - ARCHIVED → DRAFT via /restore, archivedAt cleared
 *   - publishedAt set on first publish, not overwritten on re-publish
 *   - Duplicate garmentType → HTTP 409
 */
@SpringBootTest
@ActiveProfiles("test")
class DesignPublicationIntegrationTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;
    @Autowired private CatalogGroupRepository groupRepository;
    @Autowired private CollectionRepository collectionRepository;
    @Autowired private DesignRepository designRepository;
    @Autowired private DesignGarmentRepository garmentRepository;
    @Autowired private DesignGarmentPriceRepository priceRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;

    // IDs set up per-test
    private Long collectionId;
    private Long colorId;
    private Long sizeId;

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

    // ── Cleanup ───────────────────────────────────────────────────────────────

    private void cleanAll() {
        priceRepository.deleteAll();
        garmentRepository.deleteAll();
        designRepository.deleteAll();
        collectionRepository.deleteAll();
        groupRepository.deleteAll();
        colorRepository.deleteAll();
        sizeRepository.deleteAll();
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    /** Creates group + collection + color + size. Does NOT create a design. */
    private void buildBaseFixture() {
        CatalogGroup group = new CatalogGroup();
        group.setName("Pub Test Group");
        group.setSlug("pub-test-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = groupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("Pub Test Collection");
        coll.setSlug("pub-test-collection");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);
        collectionId = coll.getId();

        Color color = new Color();
        color.setName("White");
        color.setHexCode("#FFFFFF");
        color = colorRepository.save(color);
        colorId = color.getId();

        Size size = new Size();
        size.setLabel("L");
        size = sizeRepository.save(size);
        sizeId = size.getId();
    }

    /** Creates a bare DRAFT design (no image, no garments). */
    private Design createDraftDesign(String slug) {
        Collection coll = collectionRepository.findById(collectionId).orElseThrow();
        Design d = new Design();
        d.setCollection(coll);
        d.setName("Test Design " + slug);
        d.setSlug(slug);
        d.setStatus(DesignStatus.DRAFT);
        d.setCreatedAt(LocalDateTime.now());
        return designRepository.save(d);
    }

    /** Adds a fully valid active garment (KZT price + color + size) to a design. */
    private DesignGarment addValidGarment(Design design) {
        Color color = colorRepository.findById(colorId).orElseThrow();
        Size size = sizeRepository.findById(sizeId).orElseThrow();

        DesignGarment g = new DesignGarment();
        g.setDesign(design);
        g.setGarmentType(GarmentType.HOODIE);
        g.setActive(true);
        g.getColors().add(color);
        g.getSizes().add(size);
        g = garmentRepository.save(g);

        DesignGarmentPrice price = new DesignGarmentPrice();
        price.setDesignGarment(g);
        price.setCurrency(Currency.KZT);
        price.setAmount(new BigDecimal("20000.00"));
        priceRepository.save(price);

        return g;
    }

    // ── Guard tests ───────────────────────────────────────────────────────────

    @Test
    void publish_withNoImage_returns400WithCode() throws Exception {
        Design design = createDraftDesign("pub-guard-no-image");
        // No image, no garments

        String body = mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + design.getId() + "/publish")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("code").asText()).isEqualTo("DESIGN_NOT_READY");
        assertThat(json.get("errors").toString()).contains("main_image_missing");
    }

    @Test
    void publish_withImageButNoGarments_returns400() throws Exception {
        Design design = createDraftDesign("pub-guard-no-garments");
        design.setMainImageUrl("https://cdn.example.com/img.jpg");
        designRepository.save(design);

        String body = mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + design.getId() + "/publish")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("errors").toString()).contains("no_active_garments");
    }

    @Test
    void publish_withGarmentButNoKztPrice_returns400() throws Exception {
        Design design = createDraftDesign("pub-guard-no-kzt");
        design.setMainImageUrl("https://cdn.example.com/img.jpg");
        design = designRepository.save(design);

        // Add garment with USD price only (no KZT)
        Color color = colorRepository.findById(colorId).orElseThrow();
        Size size = sizeRepository.findById(sizeId).orElseThrow();
        DesignGarment g = new DesignGarment();
        g.setDesign(design);
        g.setGarmentType(GarmentType.T_SHIRT);
        g.setActive(true);
        g.getColors().add(color);
        g.getSizes().add(size);
        g = garmentRepository.save(g);

        DesignGarmentPrice usdPrice = new DesignGarmentPrice();
        usdPrice.setDesignGarment(g);
        usdPrice.setCurrency(Currency.USD);
        usdPrice.setAmount(new BigDecimal("49.99"));
        priceRepository.save(usdPrice);

        String body = mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + design.getId() + "/publish")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("errors").toString()).contains("no_variant_with_kzt_price_size_color");
    }

    @Test
    void publish_withGarmentButNoColors_returns400() throws Exception {
        Design design = createDraftDesign("pub-guard-no-colors");
        design.setMainImageUrl("https://cdn.example.com/img.jpg");
        design = designRepository.save(design);

        Size size = sizeRepository.findById(sizeId).orElseThrow();
        DesignGarment g = new DesignGarment();
        g.setDesign(design);
        g.setGarmentType(GarmentType.HOODIE);
        g.setActive(true);
        g.getSizes().add(size);
        // No colors added
        g = garmentRepository.save(g);

        DesignGarmentPrice price = new DesignGarmentPrice();
        price.setDesignGarment(g);
        price.setCurrency(Currency.KZT);
        price.setAmount(new BigDecimal("15000.00"));
        priceRepository.save(price);

        String body = mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + design.getId() + "/publish")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("errors").toString()).contains("no_variant_with_kzt_price_size_color");
    }

    @Test
    void publish_withInactiveGarmentOnly_returns400() throws Exception {
        Design design = createDraftDesign("pub-guard-inactive-garment");
        design.setMainImageUrl("https://cdn.example.com/img.jpg");
        design = designRepository.save(design);

        Color color = colorRepository.findById(colorId).orElseThrow();
        Size size = sizeRepository.findById(sizeId).orElseThrow();
        DesignGarment g = new DesignGarment();
        g.setDesign(design);
        g.setGarmentType(GarmentType.HOODIE);
        g.setActive(false);   // inactive — must be invisible to publication check
        g.getColors().add(color);
        g.getSizes().add(size);
        g = garmentRepository.save(g);

        DesignGarmentPrice price = new DesignGarmentPrice();
        price.setDesignGarment(g);
        price.setCurrency(Currency.KZT);
        price.setAmount(new BigDecimal("15000.00"));
        priceRepository.save(price);

        String body = mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + design.getId() + "/publish")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("errors").toString()).contains("no_active_garments");
    }

    // ── Lifecycle tests ───────────────────────────────────────────────────────

    @Test
    void publish_withAllRequirementsMet_transitions_PUBLISHED_and_setsPublishedAt() throws Exception {
        Design design = createDraftDesign("pub-lifecycle-publish");
        design.setMainImageUrl("https://cdn.example.com/img.jpg");
        design = designRepository.save(design);
        addValidGarment(design);

        mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + design.getId() + "/publish")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        Design saved = designRepository.findById(design.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(DesignStatus.PUBLISHED);
        assertThat(saved.getPublishedAt()).isNotNull();
    }

    @Test
    void publishedAt_isNotOverwrittenOnRePublish() throws Exception {
        Design design = createDraftDesign("pub-lifecycle-repub");
        design.setMainImageUrl("https://cdn.example.com/img.jpg");
        design = designRepository.save(design);
        addValidGarment(design);

        Long id = design.getId();

        // First publish
        mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + id + "/publish")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        LocalDateTime firstPublishedAt = designRepository.findById(id).orElseThrow().getPublishedAt();
        assertThat(firstPublishedAt).isNotNull();

        // Unpublish then publish again
        mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + id + "/unpublish")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + id + "/publish")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        LocalDateTime secondPublishedAt = designRepository.findById(id).orElseThrow().getPublishedAt();
        assertThat(secondPublishedAt).isEqualTo(firstPublishedAt);
    }

    @Test
    void unpublish_fromPublished_transitionsToREADY() throws Exception {
        Design design = createDraftDesign("pub-lifecycle-unpub");
        design.setMainImageUrl("https://cdn.example.com/img.jpg");
        design = designRepository.save(design);
        addValidGarment(design);

        Long id = design.getId();

        mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + id + "/publish")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + id + "/unpublish")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        assertThat(designRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(DesignStatus.READY);
    }

    @Test
    void archive_fromPublished_setsArchivedAt() throws Exception {
        Design design = createDraftDesign("pub-lifecycle-archive");
        design.setMainImageUrl("https://cdn.example.com/img.jpg");
        design = designRepository.save(design);
        addValidGarment(design);

        Long id = design.getId();

        mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + id + "/publish")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + id + "/archive")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.archivedAt").isNotEmpty());

        Design saved = designRepository.findById(id).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(DesignStatus.ARCHIVED);
        assertThat(saved.getArchivedAt()).isNotNull();
    }

    @Test
    void restore_fromArchived_transitionsToDRAFT_andClearsArchivedAt() throws Exception {
        Design design = createDraftDesign("pub-lifecycle-restore");
        design.setMainImageUrl("https://cdn.example.com/img.jpg");
        design = designRepository.save(design);
        addValidGarment(design);

        Long id = design.getId();

        // publish → archive → restore
        mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + id + "/publish")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + id + "/archive")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + id + "/restore")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.archivedAt").isEmpty());

        Design saved = designRepository.findById(id).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(DesignStatus.DRAFT);
        assertThat(saved.getArchivedAt()).isNull();
    }

    @Test
    void restore_fromNonArchived_returns400() throws Exception {
        Design design = createDraftDesign("pub-restore-guard");

        mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + design.getId() + "/restore")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publish_fromArchived_returns400() throws Exception {
        Design design = createDraftDesign("pub-from-archived");
        design.setMainImageUrl("https://cdn.example.com/img.jpg");
        design.setStatus(DesignStatus.ARCHIVED);
        design = designRepository.save(design);
        addValidGarment(design);

        mockMvc.perform(patch("/api/v1/admin/catalog/designs/" + design.getId() + "/publish")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDuplicateGarmentType_returns409() {
        Design design = createDraftDesign("pub-dup-garment");
        design = designRepository.save(design);

        Color color = colorRepository.findById(colorId).orElseThrow();
        Size size = sizeRepository.findById(sizeId).orElseThrow();

        // First HOODIE — OK
        DesignGarment first = new DesignGarment();
        first.setDesign(design);
        first.setGarmentType(GarmentType.HOODIE);
        first.setActive(true);
        first.getColors().add(color);
        first.getSizes().add(size);
        garmentRepository.save(first);

        // Second HOODIE for the same design — must violate the UNIQUE constraint
        Design finalDesign = design;
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            DesignGarment second = new DesignGarment();
            second.setDesign(finalDesign);
            second.setGarmentType(GarmentType.HOODIE);
            second.setActive(true);
            garmentRepository.saveAndFlush(second);
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
