package com.nurba.java.repositories;

import com.nurba.java.domain.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    Optional<Coupon> findByCodeIgnoreCaseAndIdNot(String code, Long id);

    @Query("SELECT c FROM Coupon c WHERE :q IS NULL OR :q = '' OR UPPER(c.code) LIKE UPPER(CONCAT('%', :q, '%'))")
    Page<Coupon> search(@Param("q") String q, Pageable pageable);

    /**
     * Atomic conditional increment — closes the race window where two concurrent checkouts
     * both read usedCount &lt; maxUses and would otherwise both succeed in the application layer.
     * The WHERE clause is re-evaluated by the database against the latest committed row, so under
     * concurrent access the second UPDATE blocks on the row lock and then sees the first one's
     * incremented usedCount, correctly returning 0 once the limit is reached.
     * Returns the number of rows updated (0 = limit already reached, caller should reject).
     */
    @Modifying
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 " +
           "WHERE c.id = :id AND (c.maxUses IS NULL OR c.usedCount < c.maxUses)")
    int incrementUsageIfAllowed(@Param("id") Long id);
}
