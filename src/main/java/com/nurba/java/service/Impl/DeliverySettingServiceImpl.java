package com.nurba.java.service.Impl;

import com.nurba.java.domain.DeliverySetting;
import com.nurba.java.repositories.DeliverySettingRepository;
import com.nurba.java.service.DeliverySettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class DeliverySettingServiceImpl implements DeliverySettingService {

    /** Key for the flat Kazakhstan delivery fee. */
    public static final String KEY_KZ_DELIVERY_FLAT = "KZ_DELIVERY_FLAT_KZT";

    private final DeliverySettingRepository repository;

    /** Cold-start fallback when the row is absent (e.g. tests without Flyway). DB is authoritative. */
    @Value("${app.delivery.kz-flat-kzt-bootstrap:1600}")
    private BigDecimal kzFlatBootstrap;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal kzDeliveryFlatKzt() {
        return repository.findById(KEY_KZ_DELIVERY_FLAT)
                .map(DeliverySetting::getNumericValue)
                .orElseGet(() -> kzFlatBootstrap.setScale(2, RoundingMode.HALF_UP));
    }

    @Override
    @Transactional
    public BigDecimal setKzDeliveryFlatKzt(BigDecimal value) {
        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP);
        DeliverySetting setting = repository.findById(KEY_KZ_DELIVERY_FLAT)
                .orElseGet(DeliverySetting::new);
        setting.setSettingKey(KEY_KZ_DELIVERY_FLAT);
        setting.setNumericValue(normalized);
        repository.save(setting);
        return normalized;
    }
}
