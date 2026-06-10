package com.nurba.java.repositories;

import com.nurba.java.domain.Order;
import com.nurba.java.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
     */
    List<Order> findByStatusNotInOrderByCreatedAtDesc(Collection<OrderStatus> statuses);

    /** Unpaid orders created before the cutoff — candidates for expiry/inventory release. */
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoff);
}
