package com.nurba.java.repositories;

import com.nurba.java.domain.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    @Query("SELECT w FROM WishlistItem w JOIN FETCH w.design d JOIN FETCH d.collection c JOIN FETCH c.catalogGroup WHERE w.user.id = :userId ORDER BY w.addedAt DESC")
    List<WishlistItem> findByUserIdWithDesign(@Param("userId") Long userId);

    Optional<WishlistItem> findByUser_IdAndDesign_Id(Long userId, Long designId);

    boolean existsByUser_IdAndDesign_Id(Long userId, Long designId);

    void deleteByUser_IdAndDesign_Id(Long userId, Long designId);

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.design.id = :designId")
    void deleteByDesignId(@Param("designId") Long designId);

    long countByUser_Id(Long userId);
}
