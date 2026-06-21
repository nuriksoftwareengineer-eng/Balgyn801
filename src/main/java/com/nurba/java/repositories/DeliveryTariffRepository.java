package com.nurba.java.repositories;

import com.nurba.java.domain.DeliveryTariff;
import com.nurba.java.enums.TariffKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryTariffRepository extends JpaRepository<DeliveryTariff, Long> {

    List<DeliveryTariff> findByKindOrderByUptoKgAsc(TariffKind kind);
}
