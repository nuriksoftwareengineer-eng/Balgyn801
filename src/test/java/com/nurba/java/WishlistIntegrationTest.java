package com.nurba.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Design;
import com.nurba.java.enums.DesignStatus;
import com.nurba.java.repositories.AppUserRepository;
import com.nurba.java.repositories.CatalogGroupRepository;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.repositories.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Feature 12 — Wishlist (DB-backed, authenticated users).
 * Guest/localStorage wishlist behaviour lives entirely on the frontend and isn't covered here.
 */
@SpringBootTest
@ActiveProfiles("test")
class WishlistIntegrationTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private CatalogGroupRepository catalogGroupRepository;
    @Autowired private CollectionRepository collectionRepository;
    @Autowired private DesignRepository designRepository;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private AppUserRepository appUserRepository;

    private Long designId;
    private Long otherDesignId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        cleanAll();
        buildFixture();
    }

    private void cleanAll() {
        wishlistRepository.deleteAll();
        appUserRepository.deleteAll();
        designRepository.deleteAll();
        collectionRepository.deleteAll();
        catalogGroupRepository.deleteAll();
    }

    private void buildFixture() {
        CatalogGroup group = new CatalogGroup();
        group.setName("Wishlist Group");
        group.setSlug("wishlist-group");
        group.setActive(true);
        group.setCreatedAt(LocalDateTime.now());
        group = catalogGroupRepository.save(group);

        Collection coll = new Collection();
        coll.setCatalogGroup(group);
        coll.setName("Wishlist Collection");
        coll.setSlug("wishlist-coll");
        coll.setActive(true);
        coll.setCreatedAt(LocalDateTime.now());
        coll = collectionRepository.save(coll);

        Design design = new Design();
        design.setCollection(coll);
        design.setName("Wishlist Design");
        design.setSlug("wishlist-design");
        design.setStatus(DesignStatus.PUBLISHED);
        design.setCreatedAt(LocalDateTime.now());
        designId = designRepository.save(design).getId();

        Design other = new Design();
        other.setCollection(coll);
        other.setName("Other Design");
        other.setSlug("other-design");
        other.setStatus(DesignStatus.PUBLISHED);
        other.setCreatedAt(LocalDateTime.now());
        otherDesignId = designRepository.save(other).getId();
    }

    private String registerAndGetToken(String email) throws Exception {
        String body = """
                {"email": "%s", "password": "Password123!"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/me/wishlist"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void add_thenList_returnsItem() throws Exception {
        String token = registerAndGetToken("wish1@test.com");

        mockMvc.perform(post("/api/v1/me/wishlist/" + designId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/me/wishlist")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isEqualTo(1);
        assertThat(items.get(0).get("designId").asLong()).isEqualTo(designId);
    }

    @Test
    void add_duplicate_isRejected() throws Exception {
        String token = registerAndGetToken("wish2@test.com");

        mockMvc.perform(post("/api/v1/me/wishlist/" + designId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/me/wishlist/" + designId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void remove_deletesItem() throws Exception {
        String token = registerAndGetToken("wish3@test.com");

        mockMvc.perform(post("/api/v1/me/wishlist/" + designId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/me/wishlist/" + designId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        MvcResult result = mockMvc.perform(get("/api/v1/me/wishlist")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(items.size()).isEqualTo(0);
    }

    @Test
    void check_reflectsMembership() throws Exception {
        String token = registerAndGetToken("wish4@test.com");

        MvcResult before = mockMvc.perform(get("/api/v1/me/wishlist/check/" + designId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(before.getResponse().getContentAsString())
                .get("inWishlist").asBoolean()).isFalse();

        mockMvc.perform(post("/api/v1/me/wishlist/" + designId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MvcResult after = mockMvc.perform(get("/api/v1/me/wishlist/check/" + designId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(after.getResponse().getContentAsString())
                .get("inWishlist").asBoolean()).isTrue();
    }

    @Test
    void count_reflectsNumberOfItems() throws Exception {
        String token = registerAndGetToken("wish5@test.com");

        mockMvc.perform(post("/api/v1/me/wishlist/" + designId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/me/wishlist/" + otherDesignId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/me/wishlist/count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("count").asLong()).isEqualTo(2);
    }

    @Test
    void usersDoNotSeeEachOthersWishlists() throws Exception {
        String tokenA = registerAndGetToken("wisha@test.com");
        String tokenB = registerAndGetToken("wishb@test.com");

        mockMvc.perform(post("/api/v1/me/wishlist/" + designId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        MvcResult resultB = mockMvc.perform(get("/api/v1/me/wishlist")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(resultB.getResponse().getContentAsString()).size())
                .isEqualTo(0);
    }
}
