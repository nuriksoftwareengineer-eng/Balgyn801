package com.nurba.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Design;
import com.nurba.java.enums.DesignStatus;
import com.nurba.java.repositories.CatalogGroupRepository;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.DesignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integration tests for Features 18-20 — popular / new-arrivals / recommendations. */
@SpringBootTest
@ActiveProfiles("test")
class CatalogDiscoveryIntegrationTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;
    @Autowired private CatalogGroupRepository catalogGroupRepository;
    @Autowired private CollectionRepository collectionRepository;
    @Autowired private DesignRepository designRepository;

    private CatalogGroup group;
    private Collection collA;
    private Collection collB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        cleanAll();
        buildFixture();
    }

    private void cleanAll() {
        designRepository.deleteAll();
        collectionRepository.deleteAll();
        catalogGroupRepository.deleteAll();
    }

    private void buildFixture() {
        group = new CatalogGroup();
        group.setName("Discovery Group");
        group.setSlug("discovery-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        collA = new Collection();
        collA.setCatalogGroup(group);
        collA.setName("Discovery Collection A");
        collA.setSlug("discovery-coll-a");
        collA.setActive(true);
        collA.setCreatedAt(LocalDateTime.now());
        collA = collectionRepository.save(collA);

        collB = new Collection();
        collB.setCatalogGroup(group);
        collB.setName("Discovery Collection B");
        collB.setSlug("discovery-coll-b");
        collB.setActive(true);
        collB.setCreatedAt(LocalDateTime.now());
        collB = collectionRepository.save(collB);
    }

    private Design publishedDesign(String slug, Collection coll, int viewCount, boolean newArrival, LocalDateTime publishedAt) {
        Design d = new Design();
        d.setCollection(coll);
        d.setName("Design " + slug);
        d.setSlug(slug);
        d.setStatus(DesignStatus.PUBLISHED);
        d.setCreatedAt(LocalDateTime.now());
        d.setPublishedAt(publishedAt);
        d.setViewCount(viewCount);
        d.setNewArrival(newArrival);
        return designRepository.save(d);
    }

    // ── Popular ───────────────────────────────────────────────────────────────

    @Test
    void popular_ordersByViewCountDescending() throws Exception {
        publishedDesign("low-views", collA, 1, false, LocalDateTime.now().minusDays(60));
        publishedDesign("high-views", collA, 100, false, LocalDateTime.now().minusDays(60));
        publishedDesign("mid-views", collA, 50, false, LocalDateTime.now().minusDays(60));

        MvcResult result = mockMvc.perform(get("/api/v1/catalog/popular").param("limit", "10"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode designs = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(designs.size()).isEqualTo(3);
        assertThat(designs.get(0).get("slug").asText()).isEqualTo("high-views");
        assertThat(designs.get(1).get("slug").asText()).isEqualTo("mid-views");
        assertThat(designs.get(2).get("slug").asText()).isEqualTo("low-views");
    }

    @Test
    void popular_respectsLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            publishedDesign("popular-" + i, collA, i, false, LocalDateTime.now().minusDays(60));
        }
        MvcResult result = mockMvc.perform(get("/api/v1/catalog/popular").param("limit", "2"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode designs = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(designs.size()).isEqualTo(2);
    }

    // ── New arrivals ──────────────────────────────────────────────────────────

    @Test
    void newArrivals_includesFlaggedDesign_regardlessOfPublishDate() throws Exception {
        publishedDesign("old-but-flagged", collA, 0, true, LocalDateTime.now().minusDays(90));
        publishedDesign("old-not-flagged", collA, 0, false, LocalDateTime.now().minusDays(90));

        MvcResult result = mockMvc.perform(get("/api/v1/catalog/new-arrivals").param("limit", "10"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode designs = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(designs.size()).isEqualTo(1);
        assertThat(designs.get(0).get("slug").asText()).isEqualTo("old-but-flagged");
    }

    @Test
    void newArrivals_includesRecentlyPublished_evenWithoutFlag() throws Exception {
        publishedDesign("recent", collA, 0, false, LocalDateTime.now().minusDays(5));
        publishedDesign("old", collA, 0, false, LocalDateTime.now().minusDays(90));

        MvcResult result = mockMvc.perform(get("/api/v1/catalog/new-arrivals").param("limit", "10"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode designs = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(designs.size()).isEqualTo(1);
        assertThat(designs.get(0).get("slug").asText()).isEqualTo("recent");
    }

    // ── Recommendations ──────────────────────────────────────────────────────

    @Test
    void recommendations_prefersSameCollection_excludesSelf() throws Exception {
        Design base = publishedDesign("base-design", collA, 0, false, LocalDateTime.now().minusDays(60));
        publishedDesign("same-collection", collA, 10, false, LocalDateTime.now().minusDays(60));
        publishedDesign("other-collection", collB, 999, false, LocalDateTime.now().minusDays(60));

        // limit=1 isolates the same-collection candidate from the popular-fallback fill,
        // proving same-collection results are ranked ahead of cross-collection ones.
        MvcResult result = mockMvc.perform(get("/api/v1/catalog/recommendations")
                        .param("designId", String.valueOf(base.getId()))
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode designs = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(designs.size()).isEqualTo(1);
        assertThat(designs.get(0).get("slug").asText()).isEqualTo("same-collection");

        // At limit=6, fallback fills remaining slots from popular — but the base design
        // (the one being viewed) must never appear in its own recommendations.
        MvcResult resultWide = mockMvc.perform(get("/api/v1/catalog/recommendations")
                        .param("designId", String.valueOf(base.getId()))
                        .param("limit", "6"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode wideDesigns = objectMapper.readTree(resultWide.getResponse().getContentAsString());
        assertThat(wideDesigns.size()).isEqualTo(2);
        for (JsonNode d : wideDesigns) {
            assertThat(d.get("id").asLong()).isNotEqualTo(base.getId());
        }
    }

    @Test
    void recommendations_fallsBackToPopular_whenSameCollectionInsufficient() throws Exception {
        Design base = publishedDesign("base-design2", collA, 0, false, LocalDateTime.now().minusDays(60));
        publishedDesign("popular-other", collB, 500, false, LocalDateTime.now().minusDays(60));

        MvcResult result = mockMvc.perform(get("/api/v1/catalog/recommendations")
                        .param("designId", String.valueOf(base.getId()))
                        .param("limit", "6"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode designs = objectMapper.readTree(result.getResponse().getContentAsString());
        // No same-collection candidates — fallback to popular must still surface a result,
        // and must never include the base design itself.
        assertThat(designs.size()).isGreaterThanOrEqualTo(1);
        for (JsonNode d : designs) {
            assertThat(d.get("id").asLong()).isNotEqualTo(base.getId());
        }
    }
}
