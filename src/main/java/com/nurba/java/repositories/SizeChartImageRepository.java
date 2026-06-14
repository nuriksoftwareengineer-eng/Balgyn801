package com.nurba.java.repositories;

import com.nurba.java.domain.SizeChartImage;
import com.nurba.java.enums.GarmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SizeChartImageRepository extends JpaRepository<SizeChartImage, Long> {
    List<SizeChartImage> findAllByActiveTrueOrderByGarmentTypeAsc();
    Optional<SizeChartImage> findByGarmentType(GarmentType garmentType);
}
