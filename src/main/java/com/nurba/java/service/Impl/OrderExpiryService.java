package com.nurba.java.service.Impl;

import com.nurba.java.domain.Inventory;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.OrderHistory;
import com.nurba.java.domain.OrderItem;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.repositories.InventoryRepository;
import com.nurba.java.repositories.OrderHistoryRepository;
import com.nurba.java.repositories.OrderItemRepository;
import com.nurba.java.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * Expires unpaid orders (status {@code PENDING_PAYMENT}) once the payment window elapses.
 * <p>
 * For each stale order it releases the reserved inventory back to stock (under the same
 * pessimistic lock used at reservation), marks the order {@code EXPIRED}, and writes an audit
 * history entry. The order row is retained for audit and is excluded from the admin list.
 */
@Service
@RequiredArgsConstructor
public class OrderExpiryService {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderHistoryRepository orderHistoryRepository;

    /** Minutes an order may remain unpaid before it expires. */
    @Value("${app.orders.payment-window-minutes:60}")
    private long paymentWindowMinutes;

    /**
     * Scans for expired unpaid orders. Runs on a fixed delay (default 5 min); the per-order work
     * is idempotent, so overlapping or repeated runs are safe.
     */
    @Scheduled(fixedDelayString = "${app.orders.expiry-scan-ms:300000}",
            initialDelayString = "${app.orders.expiry-initial-delay-ms:60000}")
    @Transactional
    public void expireStaleOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(paymentWindowMinutes);
        List<Order> stale = orderRepository
                .findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, cutoff);
        if (stale.isEmpty()) {
            return;
        }
        for (Order order : stale) {
            expire(order);
        }
        log.info("Expired {} unpaid order(s) older than {} min; inventory released.",
                stale.size(), paymentWindowMinutes);
    }

    /**
     * Expires a single order: releases inventory, marks EXPIRED, records history.
     * Public + transactional so it can also be invoked directly (e.g. tests, reconciliation).
     */
    @Transactional
    public void expire(Order order) {
        releaseInventory(order);
        order.setStatus(OrderStatus.EXPIRED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        recordHistory(order);
    }

    private void releaseInventory(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
        for (OrderItem item : items) {
            // Only design-based items reserve inventory; legacy product items hold none.
            if (item.getDesignGarment() == null || item.getColor() == null || item.getSize() == null) {
                continue;
            }
            int qty = item.getQuantity() == null ? 0 : item.getQuantity();
            if (qty <= 0) {
                continue;
            }
            inventoryRepository.findAndLockByGarmentColorSize(
                            item.getDesignGarment().getId(),
                            item.getColor().getId(),
                            item.getSize().getId())
                    .ifPresent(inv -> {
                        inv.setQuantity(inv.getQuantity() + qty);
                        inventoryRepository.save(inv);
                    });
        }
    }

    private void recordHistory(Order order) {
        OrderHistory entry = new OrderHistory();
        entry.setOrder(order);
        entry.setStatus(OrderStatus.EXPIRED);
        entry.setDateAdded(new Date());
        orderHistoryRepository.save(entry);
    }
}
