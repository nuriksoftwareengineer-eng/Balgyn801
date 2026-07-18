package com.nurba.java.repositories;

import com.nurba.java.domain.Design;
import com.nurba.java.enums.DesignStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DesignRepository extends JpaRepository<Design, Long> {
    boolean existsBySlug(String slug);
    boolean existsByName(String name);
    boolean existsByCollection_Id(Long collectionId);
    long countByCollection_Id(Long collectionId);
    List<Design> findByCollection_Id(Long collectionId);
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

    // Popular designs: sorted by viewCount DESC (PUBLISHED only)
    @EntityGraph(attributePaths = {"collection", "collection.catalogGroup"})
    @Query("SELECT d FROM Design d WHERE d.status = 'PUBLISHED' ORDER BY d.viewCount DESC")
    List<Design> findTopByViewCount(org.springframework.data.domain.Pageable pageable);

    // New arrivals: isNewArrival flag OR published in last N days
    @EntityGraph(attributePaths = {"collection", "collection.catalogGroup"})
    @Query("SELECT d FROM Design d WHERE d.status = 'PUBLISHED' AND (d.isNewArrival = true OR d.publishedAt >= :since) ORDER BY d.publishedAt DESC NULLS LAST")
    List<Design> findNewArrivals(@Param("since") LocalDateTime since, org.springframework.data.domain.Pageable pageable);

    // Recommendations: same collection, PUBLISHED, excluding current design
    @EntityGraph(attributePaths = {"collection", "collection.catalogGroup"})
    @Query("SELECT d FROM Design d WHERE d.status = 'PUBLISHED' AND d.collection.id = :collectionId AND d.id <> :excludeId ORDER BY d.viewCount DESC")
    List<Design> findRecommendations(@Param("collectionId") Long collectionId, @Param("excludeId") Long excludeId, org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("UPDATE Design d SET d.viewCount = d.viewCount + 1 WHERE d.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
