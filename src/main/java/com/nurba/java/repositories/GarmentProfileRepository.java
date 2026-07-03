package com.nurba.java.repositories;

import com.nurba.java.domain.GarmentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GarmentProfileRepository extends JpaRepository<GarmentProfile, Long> {

    List<GarmentProfile> findAllByOrderBySortOrderAscNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
