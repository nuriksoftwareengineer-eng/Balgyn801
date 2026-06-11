package com.nurba.java;

import com.nurba.java.domain.Country;
import com.nurba.java.enums.ShippingZone;
import com.nurba.java.repositories.CountryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase A — GET /api/v1/delivery/methods
 *
 * Verifies the zone→method matrix and per-method metadata that the frontend
 * renders verbatim. No method names, labels, or city restrictions are
 * hardcoded client-side; everything comes from this endpoint.
 */
@SpringBootTest
@ActiveProfiles("test")
class DeliveryMethodsIntegrationTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;
    @Autowired private CountryRepository countryRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        countryRepository.deleteAll();
        countryRepository.save(country("KZ", "Казахстан", "Kazakhstan", ShippingZone.KAZAKHSTAN));
        countryRepository.save(country("RU", "Россия",   "Russia",     ShippingZone.CIS));
        countryRepository.save(country("DE", "Германия", "Germany",    ShippingZone.INTERNATIONAL));
    }

    @AfterEach
    void tearDown() {
        countryRepository.deleteAll();
    }

    private Country country(String iso2, String nameRu, String nameEn, ShippingZone zone) {
        Country c = new Country();
        c.setIso2(iso2);
        c.setNameRu(nameRu);
        c.setNameEn(nameEn);
        c.setShippingZone(zone);
        c.setActive(true);
        return c;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Kazakhstan zone must return all four methods: PICKUP, TAXI (Almaty),
     * POSTAL (Kazpost), and CDEK. Order is implementation-defined; only
     * presence of all four types is asserted.
     */
    @Test
    void kazakhstan_returnsPickupTaxiPostalAndCdek() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/methods").param("countryIso2", "KZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].type",
                        containsInAnyOrder("PICKUP", "TAXI", "POSTAL", "CDEK")));
    }

    /**
     * CIS zone (e.g. Russia) must return exactly one method: CDEK.
     * POSTAL was removed from CIS per business requirements.
     */
    @Test
    void cis_returnsOnlyCdek() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/methods").param("countryIso2", "RU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("CDEK"));
    }

    /**
     * International zone (e.g. Germany) must return exactly one method: INTERNATIONAL.
     */
    @Test
    void international_returnsOnlyInternational() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/methods").param("countryIso2", "DE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("INTERNATIONAL"));
    }

    /**
     * PICKUP must not require any address, city search, or PVZ.
     * Fee is 0 (free) and no city restriction is set — it is available
     * to any KZ customer regardless of city.
     *
     * JSONPath note: filter expressions return a list; Spring MockMvc automatically
     * unwraps a single-element list when comparing with a scalar value. Do NOT append
     * [0] after a filter — Jayway does not support filter-then-index chaining.
     */
    @Test
    void pickup_requiresNoAddress() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/methods").param("countryIso2", "KZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='PICKUP')].requiresAddress").value(false))
                .andExpect(jsonPath("$[?(@.type=='PICKUP')].requiresCitySearch").value(false))
                .andExpect(jsonPath("$[?(@.type=='PICKUP')].requiresPvz").value(false))
                .andExpect(jsonPath("$[?(@.type=='PICKUP')].estimatedFeeKzt").value(0.0));
    }

    /**
     * CDEK must require both a city search (autocomplete widget) and a PVZ selector.
     * It also requires a delivery address; fee is null (weight- and city-dependent).
     */
    @Test
    void cdek_requiresCitySearchAndPvz() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/methods").param("countryIso2", "KZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='CDEK')].requiresCitySearch").value(true))
                .andExpect(jsonPath("$[?(@.type=='CDEK')].requiresPvz").value(true))
                .andExpect(jsonPath("$[?(@.type=='CDEK')].requiresAddress").value(true));
    }

    /**
     * TAXI must carry an Almaty city restriction surfaced from the backend.
     * The frontend must display it and must never hardcode the city name.
     * Fee is the flat Kazakhstan delivery rate (1600 KZT bootstrap default).
     */
    @Test
    void taxi_containsAlmatyRestriction() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/methods").param("countryIso2", "KZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='TAXI')].cityRestriction").value("Алматы"))
                .andExpect(jsonPath("$[?(@.type=='TAXI')].requiresAddress").value(true))
                .andExpect(jsonPath("$[?(@.type=='TAXI')].requiresCitySearch").value(false))
                .andExpect(jsonPath("$[?(@.type=='TAXI')].requiresPvz").value(false))
                .andExpect(jsonPath("$[?(@.type=='TAXI')].estimatedFeeKzt").value(1600.0));
    }
}
