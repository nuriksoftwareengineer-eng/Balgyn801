package com.nurba.java.repositories;

import com.nurba.java.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByDesign_IdOrderByCreatedAtDesc(Long designId);

    boolean existsByDesign_IdAndAppUser_Id(Long designId, Long userId);

    @Modifying
    @Query("DELETE FROM Review r WHERE r.design.id = :designId")
    void deleteByDesignId(@Param("designId") Long designId);
}
