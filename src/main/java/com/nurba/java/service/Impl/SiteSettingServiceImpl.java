package com.nurba.java.service.Impl;

import com.nurba.java.domain.SiteSetting;
import com.nurba.java.repositories.SiteSettingRepository;
import com.nurba.java.service.SiteSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Keys that the public storefront may access without authentication. */
class PublicSettingKeys {
    static final List<String> ALL = List.of(
            "ceo_photo_url",
            "production_description",
            "delivery_description",
            "shipping_description",
            "care_instructions");
}

@Service
@RequiredArgsConstructor
public class SiteSettingServiceImpl implements SiteSettingService {

    private final SiteSettingRepository repository;

    @Override
    @Transactional(readOnly = true)
    public String get(String key) {
        return repository.findById(key).map(SiteSetting::getValue).orElse(null);
    }

    @Override
    @Transactional
    public String set(String key, String value) {
        SiteSetting setting = repository.findById(key)
                .orElse(new SiteSetting(key, null, LocalDateTime.now()));
        setting.setValue(value);
        setting.setUpdatedAt(LocalDateTime.now());
        return repository.save(setting).getValue();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getPublicSettings() {
        return repository.findAllById(PublicSettingKeys.ALL)
                .stream()
                .collect(Collectors.toMap(SiteSetting::getKey, s -> s.getValue() != null ? s.getValue() : ""));
    }
}
