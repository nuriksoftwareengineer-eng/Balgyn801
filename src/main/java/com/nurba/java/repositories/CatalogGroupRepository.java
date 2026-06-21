package com.nurba.java.repositories;

import com.nurba.java.domain.CatalogGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CatalogGroupRepository extends JpaRepository<CatalogGroup, Long> {
    Optional<CatalogGroup> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByName(String name);

    // Storefront — active only, sorted
    List<CatalogGroup> findAllByActiveTrueOrderBySortOrderAsc();
    Optional<CatalogGroup> findBySlugAndActiveTrue(String slug);
}
