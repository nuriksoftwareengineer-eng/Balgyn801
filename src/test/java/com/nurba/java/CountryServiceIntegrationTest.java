package com.nurba.java;

import com.nurba.java.dto.request.UpsertCountryRequest;
import com.nurba.java.dto.responce.AdminCountryResponse;
import com.nurba.java.dto.responce.CountryResponse;
import com.nurba.java.enums.ShippingZone;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.repositories.CountryRepository;
import com.nurba.java.service.CountryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 2 — country list is backend-controlled; the customer picks by ISO2 and the backend
 * resolves the zone. The public view never exposes the zone.
 */
@SpringBootTest
@ActiveProfiles("test")
class CountryServiceIntegrationTest {

    @Autowired private CountryService service;
    @Autowired private CountryRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void resolveZone_isCaseInsensitive() {
        service.create(new UpsertCountryRequest("kz", "Казахстан", "Kazakhstan", ShippingZone.KAZAKHSTAN, true));
        assertThat(service.resolveZone("KZ")).isEqualTo(ShippingZone.KAZAKHSTAN);
        assertThat(service.resolveZone("kz")).isEqualTo(ShippingZone.KAZAKHSTAN);
    }

    @Test
    void listActive_excludesInactive() {
        service.create(new UpsertCountryRequest("KZ", "Казахстан", "Kazakhstan", ShippingZone.KAZAKHSTAN, true));
        AdminCountryResponse ru =
                service.create(new UpsertCountryRequest("RU", "Россия", "Russia", ShippingZone.CIS, true));
        service.update(ru.id(), new UpsertCountryRequest("RU", "Россия", "Russia", ShippingZone.CIS, false));

        List<CountryResponse> active = service.listActive();
        assertThat(active).extracting(CountryResponse::iso2).containsExactly("KZ");
    }

    @Test
    void requireActiveByIso2_rejectsUnknownAndInactive() {
        assertThatThrownBy(() -> service.requireActiveByIso2("ZZ"))
                .isInstanceOf(BusinessRuleException.class);

        service.create(new UpsertCountryRequest("DE", "Германия", "Germany", ShippingZone.INTERNATIONAL, false));
        assertThatThrownBy(() -> service.requireActiveByIso2("DE"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_duplicateIso2_rejected_caseInsensitive() {
        service.create(new UpsertCountryRequest("US", "США", "United States", ShippingZone.INTERNATIONAL, true));
        assertThatThrownBy(() ->
                service.create(new UpsertCountryRequest("us", "США", "United States", ShippingZone.INTERNATIONAL, true)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
