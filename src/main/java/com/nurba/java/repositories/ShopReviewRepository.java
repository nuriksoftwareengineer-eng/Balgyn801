package com.nurba.java.repositories;

import com.nurba.java.domain.ShopReview;
import com.nurba.java.enums.ShopReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopReviewRepository extends JpaRepository<ShopReview, Long> {

    Page<ShopReview> findByStatus(ShopReviewStatus status, Pageable pageable);

    @Query("""
            SELECT r FROM ShopReview r WHERE
            LOWER(r.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(r.body) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(COALESCE(r.city, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<ShopReview> search(@Param("q") String q, Pageable pageable);
}
