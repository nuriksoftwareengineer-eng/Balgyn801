package com.nurba.java.repositories;

import com.nurba.java.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByDesign_IdOrderByCreatedAtDesc(Long designId);

    boolean existsByDesign_IdAndAppUser_Id(Long designId, Long userId);
}
