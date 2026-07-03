package com.nurba.java.repositories;

import com.nurba.java.domain.RefreshTokenRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenRecord, Long> {

    Optional<RefreshTokenRecord> findByJti(String jti);

    /** Revokes every still-active refresh token for a user (logout / reuse-detection). */
    @Modifying
    @Query("update RefreshTokenRecord r set r.revoked = true where r.userId = :userId and r.revoked = false")
    int revokeAllForUser(@Param("userId") Long userId);

    /** Housekeeping: drop long-expired rows so the table doesn't grow unbounded. */
    @Modifying
    @Query("delete from RefreshTokenRecord r where r.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
