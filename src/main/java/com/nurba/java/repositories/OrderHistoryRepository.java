package com.nurba.java.repositories;

import com.nurba.java.domain.OrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {

    /** All history entries for an order, newest first. */
    List<OrderHistory> findByOrder_IdOrderByDateAddedDesc(Long orderId);
}
