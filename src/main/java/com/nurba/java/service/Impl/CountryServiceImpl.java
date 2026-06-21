package com.nurba.java.service.Impl;

import com.nurba.java.domain.Country;
import com.nurba.java.dto.request.UpsertCountryRequest;
import com.nurba.java.dto.responce.AdminCountryResponse;
import com.nurba.java.dto.responce.CountryResponse;
import com.nurba.java.enums.ShippingZone;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.repositories.CountryRepository;
import com.nurba.java.service.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {

    private final CountryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<CountryResponse> listActive() {
        return repository.findByActiveTrueOrderByNameRuAsc()
                .stream()
                .map(c -> new CountryResponse(c.getIso2(), c.getNameRu(), c.getNameEn()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminCountryResponse> listAll() {
        return repository.findAll().stream().map(this::toAdmin).toList();
    }

    @Override
    @Transactional
    public AdminCountryResponse create(UpsertCountryRequest request) {
        String iso2 = normalizeIso2(request.iso2());
        if (repository.existsByIso2IgnoreCase(iso2)) {
            throw new BusinessRuleException("Страна с кодом " + iso2 + " уже существует");
        }
        Country country = new Country();
        apply(country, request, iso2);
        return toAdmin(repository.save(country));
    }

    @Override
    @Transactional
    public AdminCountryResponse update(Long id, UpsertCountryRequest request) {
        Country country = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Страна не найдена: " + id));
        String iso2 = normalizeIso2(request.iso2());
        // If the code changed, ensure the new one isn't taken by a different row.
        if (!country.getIso2().equalsIgnoreCase(iso2) && repository.existsByIso2IgnoreCase(iso2)) {
            throw new BusinessRuleException("Страна с кодом " + iso2 + " уже существует");
        }
        apply(country, request, iso2);
        return toAdmin(repository.save(country));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Страна не найдена: " + id);
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Country requireActiveByIso2(String iso2) {
        if (iso2 == null || iso2.isBlank()) {
            throw new BusinessRuleException("Укажите страну доставки");
        }
        Country country = repository.findByIso2IgnoreCase(iso2.trim())
                .orElseThrow(() -> new BusinessRuleException(
                        "Доставка в выбранную страну недоступна"));
        if (Boolean.FALSE.equals(country.getActive())) {
            throw new BusinessRuleException("Доставка в выбранную страну недоступна");
        }
        return country;
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingZone resolveZone(String iso2) {
        return requireActiveByIso2(iso2).getShippingZone();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void apply(Country country, UpsertCountryRequest request, String iso2) {
        country.setIso2(iso2);
        country.setNameRu(request.nameRu().trim());
        country.setNameEn(request.nameEn().trim());
        country.setShippingZone(request.shippingZone());
        country.setActive(request.active() == null ? Boolean.TRUE : request.active());
    }

    private static String normalizeIso2(String iso2) {
        return iso2 == null ? null : iso2.trim().toUpperCase(Locale.ROOT);
    }

    private AdminCountryResponse toAdmin(Country c) {
        return new AdminCountryResponse(
                c.getId(), c.getIso2(), c.getNameRu(), c.getNameEn(),
                c.getShippingZone(), Boolean.TRUE.equals(c.getActive()));
    }
}
