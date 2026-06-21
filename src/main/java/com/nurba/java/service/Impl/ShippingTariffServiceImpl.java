package com.nurba.java.service.Impl;

import com.nurba.java.domain.DeliveryTariff;
import com.nurba.java.enums.TariffKind;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.repositories.DeliveryTariffRepository;
import com.nurba.java.service.ShippingTariffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShippingTariffServiceImpl implements ShippingTariffService {

    private final DeliveryTariffRepository repository;

    /** Cold-start fallback brackets (mirrors the V16 seed) used when the table is empty (e.g. tests). */
    private static final Map<TariffKind, List<Bracket>> FALLBACK = Map.of(
            TariffKind.AIR, List.of(
                    new Bracket("0.5", "3000"),
                    new Bracket("1.0", "4000"),
                    new Bracket("2.0", "6000"),
                    new Bracket("5.0", "10000"),
                    new Bracket("10.0", "18000")),
            TariffKind.POSTAL, List.of(
                    new Bracket("0.5", "2000"),
                    new Bracket("1.0", "2800"),
                    new Bracket("2.0", "4000"),
                    new Bracket("5.0", "7000"),
                    new Bracket("10.0", "12000")));

    @Override
    @Transactional(readOnly = true)
    public BigDecimal baseFeeKzt(TariffKind kind, BigDecimal weightKg) {
        BigDecimal weight = weightKg == null ? BigDecimal.ZERO : weightKg;

        List<DeliveryTariff> rows = repository.findByKindOrderByUptoKgAsc(kind);
        if (!rows.isEmpty()) {
            for (DeliveryTariff t : rows) {
                if (weight.compareTo(t.getUptoKg()) <= 0) {
                    return t.getBaseKzt().setScale(2, RoundingMode.HALF_UP);
                }
            }
        } else {
            for (Bracket b : FALLBACK.getOrDefault(kind, List.of())) {
                if (weight.compareTo(b.uptoKg()) <= 0) {
                    return b.baseKzt().setScale(2, RoundingMode.HALF_UP);
                }
            }
        }
        throw new BusinessRuleException(
                "Свяжитесь с поддержкой: вес заказа превышает доступные тарифы доставки");
    }

    private record Bracket(BigDecimal uptoKg, BigDecimal baseKzt) {
        Bracket(String uptoKg, String baseKzt) {
            this(new BigDecimal(uptoKg), new BigDecimal(baseKzt));
        }
    }
}
