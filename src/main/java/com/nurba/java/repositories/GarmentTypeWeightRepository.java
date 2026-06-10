package com.nurba.java.repositories;

import com.nurba.java.domain.GarmentTypeWeight;
import com.nurba.java.enums.GarmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GarmentTypeWeightRepository extends JpaRepository<GarmentTypeWeight, GarmentType> {
}
