package com.nurba.java.repositories;

import com.nurba.java.domain.Design;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignRepository extends JpaRepository<Design, Long> {
    List<Design> findByCollection_Id(Long collectionId);
    Optional<Design> findBySlug(String slug);
    boolean existsBySlug(String slug);

    // Storefront — active only
    List<Design> findByCollection_IdAndActiveTrueOrderByCreatedAtDesc(Long collectionId);
    List<Design> findAllByActiveTrueOrderByCreatedAtDesc();
    Optional<Design> findBySlugAndActiveTrue(String slug);
}
