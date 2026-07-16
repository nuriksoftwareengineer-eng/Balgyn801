package com.nurba.java.repositories;

import com.nurba.java.domain.IntlZoneTariff;
import com.nurba.java.enums.IntlShipKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntlZoneTariffRepository extends JpaRepository<IntlZoneTariff, Long> {

    Optional<IntlZoneTariff> findByZoneAndKind(String zone, IntlShipKind kind);
}
