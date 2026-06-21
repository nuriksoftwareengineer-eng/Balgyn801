package com.nurba.java.repositories;

import com.nurba.java.domain.Design;
import com.nurba.java.enums.DesignStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DesignRepository extends JpaRepository<Design, Long> {
    boolean existsBySlug(String slug);
    Optional<Design> findBySlug(String slug);

    // Storefront — PUBLISHED only, with collection + catalogGroup eagerly loaded (prevents N+1)
    @EntityGraph(attributePaths = {"collection", "collection.catalogGroup"})
    List<Design> findByCollection_IdAndStatusOrderByCreatedAtDesc(Long collectionId, DesignStatus status);

    @EntityGraph(attributePaths = {"collection", "collection.catalogGroup"})
    List<Design> findAllByStatusOrderByCreatedAtDesc(DesignStatus status);

    @EntityGraph(attributePaths = {"collection", "collection.catalogGroup"})
    Optional<Design> findBySlugAndStatus(String slug, DesignStatus status);

    // Admin — all designs with garments eagerly loaded (avoids N+1 for garment count)
    @EntityGraph(attributePaths = "garments")
    @Query("SELECT d FROM Design d ORDER BY d.createdAt DESC NULLS LAST")
    List<Design> findAllWithGarments();

    @EntityGraph(attributePaths = "garments")
    @Query("SELECT d FROM Design d WHERE d.collection.id = :collectionId ORDER BY d.createdAt DESC NULLS LAST")
    List<Design> findByCollectionIdWithGarments(@Param("collectionId") Long collectionId);
}
