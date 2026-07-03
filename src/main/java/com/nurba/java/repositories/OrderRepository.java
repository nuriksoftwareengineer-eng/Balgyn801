package com.nurba.java.repositories;

import com.nurba.java.domain.Order;
import com.nurba.java.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** All orders placed by a specific authenticated user, newest first. */
    List<Order> findByAppUser_IdOrderByCreatedAtDesc(Long userId);

    /**
     * Admin order list: excludes the given statuses (PENDING_PAYMENT and EXPIRED),
     * so unpaid/abandoned orders never reach the admin panel. Newest first.
     * EntityGraph eagerly fetches customer, deliveryAddress and orderItems in a single JOIN
     * query, eliminating the N+1 selects that occur with LAZY defaults.
     */
    @EntityGraph(attributePaths = {"customer", "deliveryAddress", "orderItems"})
    List<Order> findByStatusNotInOrderByCreatedAtDesc(Collection<OrderStatus> statuses);

    /** Unpaid orders created before the cutoff — candidates for expiry/inventory release. */
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoff);

    /** Admin paginated search: excludes PENDING_PAYMENT + EXPIRED, filters by id or customer name/phone. */
    @EntityGraph(attributePaths = {"customer", "deliveryAddress", "orderItems"})
    @Query("""
            SELECT o FROM Order o
            WHERE o.status NOT IN :excludeStatuses
              AND (:q IS NULL OR :q = ''
                   OR CAST(o.id AS string) LIKE CONCAT('%', :q, '%')
                   OR LOWER(o.customer.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(o.customer.phone) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY o.createdAt DESC
            """)
    Page<Order> searchAdmin(
            @Param("excludeStatuses") Collection<OrderStatus> excludeStatuses,
            @Param("q") String q,
            Pageable pageable);
}
