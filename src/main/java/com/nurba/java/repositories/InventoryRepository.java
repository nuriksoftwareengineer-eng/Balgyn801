package com.nurba.java.repositories;

import com.nurba.java.domain.Inventory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findByDesignGarment_Id(Long designGarmentId);

    /** Bulk fetch: avoids N+1 when loading stockMaps for all garments of a design. */
    List<Inventory> findByDesignGarment_IdIn(Collection<Long> designGarmentIds);
    Optional<Inventory> findByDesignGarment_IdAndColor_IdAndSize_Id(Long designGarmentId, Long colorId, Long sizeId);

    /**
     * Acquires a pessimistic write lock (SELECT … FOR UPDATE) on the inventory row
     * for the given garment/color/size combination.
     * <p>
     * The lock is held until the caller's transaction commits or rolls back.
     * Concurrent callers block here, then re-read the already-decremented quantity,
     * preventing overselling without application-level retries.
     */
    @Modifying
    @Query("DELETE FROM Inventory i WHERE i.designGarment.id IN :ids")
    void deleteByDesignGarmentIds(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT i FROM Inventory i WHERE i.designGarment.id = :garmentId AND i.color.id = :colorId AND i.size.id = :sizeId")
    Optional<Inventory> findAndLockByGarmentColorSize(
            @Param("garmentId") Long garmentId,
            @Param("colorId") Long colorId,
            @Param("sizeId") Long sizeId);
}
