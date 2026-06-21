package com.nurba.java.repositories;

import com.nurba.java.domain.DesignGarmentPrice;
import com.nurba.java.enums.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignGarmentPriceRepository extends JpaRepository<DesignGarmentPrice, Long> {
    List<DesignGarmentPrice> findByDesignGarment_Id(Long designGarmentId);
    Optional<DesignGarmentPrice> findByDesignGarment_IdAndCurrency(Long designGarmentId, Currency currency);
}
