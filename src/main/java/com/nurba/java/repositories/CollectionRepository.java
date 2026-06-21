package com.nurba.java.repositories;

import com.nurba.java.domain.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    List<Collection> findByCatalogGroup_Id(Long groupId);
    Optional<Collection> findBySlug(String slug);
    boolean existsBySlug(String slug);

    // Storefront — active only
    List<Collection> findByCatalogGroup_IdAndActiveTrueOrderBySortOrderAsc(Long groupId);
    Optional<Collection> findBySlugAndActiveTrue(String slug);
}
