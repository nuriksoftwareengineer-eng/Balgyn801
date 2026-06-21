package com.nurba.java.repositories;

import com.nurba.java.domain.DesignGarment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DesignGarmentRepository extends JpaRepository<DesignGarment, Long> {
    List<DesignGarment> findByDesign_Id(Long designId);

    // Storefront — active only
    List<DesignGarment> findByDesign_IdAndActiveTrue(Long designId);
}
