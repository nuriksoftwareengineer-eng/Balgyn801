package com.nurba.java.repositories;

import com.nurba.java.domain.IntlZoneTariff;
import com.nurba.java.enums.IntlShipKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntlZoneTariffRepository extends JpaRepository<IntlZoneTariff, Long> {

    /** Весовые пороги зоны/типа перевозки, по возрастанию — для поиска первого подходящего. */
    List<IntlZoneTariff> findByZoneAndKindAndIncrementFalseOrderByUptoKgAsc(String zone, IntlShipKind kind);

    /** Надбавка за каждый доп. кг сверх максимального порога («+1 кг»). */
    Optional<IntlZoneTariff> findByZoneAndKindAndIncrementTrue(String zone, IntlShipKind kind);
}
