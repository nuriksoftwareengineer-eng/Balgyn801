package com.nurba.java.repositories;

import com.nurba.java.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** Load all items for an order — used by inventory deduction on payment success. */
    List<OrderItem> findByOrder_Id(Long orderId);

    /**
     * Returns true when the given user has at least one DELIVERED order
     * that contains a design-garment belonging to the specified design.
     * Used to gate review creation.
     */
    @Query("""
            SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END
            FROM OrderItem oi
            WHERE oi.order.appUser.id   = :userId
              AND oi.order.status       = com.nurba.java.enums.OrderStatus.DELIVERED
              AND oi.designGarment.design.id = :designId
            """)
    boolean hasPurchasedDesign(@Param("userId") Long userId,
                               @Param("designId") Long designId);

    boolean existsByDesignGarment_Design_Id(Long designId);
}
