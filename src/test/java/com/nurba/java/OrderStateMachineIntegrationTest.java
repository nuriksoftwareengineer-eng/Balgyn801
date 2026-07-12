package com.nurba.java;

import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Color;
import com.nurba.java.domain.Design;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.DesignGarmentPrice;
import com.nurba.java.domain.Inventory;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.Size;
import com.nurba.java.dto.request.UpdateOrderStatusRequest;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.DeliveryType;
import com.nurba.java.enums.GarmentType;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.repositories.CatalogGroupRepository;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.ColorRepository;
import com.nurba.java.repositories.CustomerRepository;
import com.nurba.java.repositories.DesignGarmentPriceRepository;
import com.nurba.java.repositories.DesignGarmentRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.repositories.InventoryRepository;
import com.nurba.java.repositories.OrderHistoryRepository;
import com.nurba.java.repositories.OrderItemRepository;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.repositories.PaymentRepository;
import com.nurba.java.repositories.SizeRepository;
import com.nurba.java.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the order status transition allowlist introduced to prevent:
 *  - Admin confirming an unpaid order (PENDING_PAYMENT → CONFIRMED blocked)
 *  - Backwards transitions (SHIPPED → CONFIRMED, IN_PRODUCTION → NEW etc.)
 *  - Skipping production steps (CONFIRMED → DELIVERED without SHIPPED)
 *
 * System transitions (PENDING_PAYMENT → CONFIRMED by payment, → EXPIRED by expiry)
 * bypass updateOrderStatus() and are NOT tested here — they are covered by
 * InventoryReleaseIntegrationTest and PayPalPaymentIntegrationTest.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderStateMachineIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderHistoryRepository orderHistoryRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private DesignGarmentPriceRepository designGarmentPriceRepository;
    @Autowired private DesignGarmentRepository designGarmentRepository;
    @Autowired private DesignRepository designRepository;
    @Autowired private CollectionRepository collectionRepository;
    @Autowired private CatalogGroupRepository catalogGroupRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;

    @BeforeEach
    @AfterEach
    void cleanAll() {
        orderHistoryRepository.deleteAll();
        paymentRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();
        inventoryRepository.deleteAll();
        designGarmentPriceRepository.deleteAll();
        designGarmentRepository.deleteAll();
        designRepository.deleteAll();
        collectionRepository.deleteAll();
        catalogGroupRepository.deleteAll();
        colorRepository.deleteAll();
        sizeRepository.deleteAll();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Creates a bare order in the given status, no items or customer required. */
    private long createOrderWithStatus(OrderStatus status) {
        Order order = new Order();
        order.setStatus(status);
        order.setDeliveryType(DeliveryType.PICKUP);
        order.setTotalPrice(BigDecimal.ZERO);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order).getId();
    }

    private void assertTransitionAllowed(long orderId, OrderStatus target) {
        orderService.updateOrderStatus(orderId, UpdateOrderStatusRequest.builder().status(target).build());
        Order updated = orderRepository.findById(orderId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(target);
    }

    private void assertTransitionBlocked(long orderId, OrderStatus target) {
        assertThatThrownBy(() ->
                orderService.updateOrderStatus(orderId, UpdateOrderStatusRequest.builder().status(target).build())
        ).isInstanceOf(BusinessRuleException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRITICAL: admin CANNOT confirm an unpaid order
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void pendingPayment_cannotBeConfirmedByAdmin() {
        long orderId = createOrderWithStatus(OrderStatus.PENDING_PAYMENT);
        assertTransitionBlocked(orderId, OrderStatus.CONFIRMED);
    }

    @Test
    void pendingPayment_canBeCancelledByAdmin() {
        long orderId = createOrderWithStatus(OrderStatus.PENDING_PAYMENT);
        assertTransitionAllowed(orderId, OrderStatus.CANCELLED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Forward-only production pipeline
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void confirmed_canAdvanceToInProduction() {
        long orderId = createOrderWithStatus(OrderStatus.CONFIRMED);
        assertTransitionAllowed(orderId, OrderStatus.IN_PRODUCTION);
    }

    @Test
    void confirmed_cannotSkipToShipped() {
        long orderId = createOrderWithStatus(OrderStatus.CONFIRMED);
        assertTransitionBlocked(orderId, OrderStatus.SHIPPED);
    }

    @Test
    void confirmed_cannotSkipToDelivered() {
        long orderId = createOrderWithStatus(OrderStatus.CONFIRMED);
        assertTransitionBlocked(orderId, OrderStatus.DELIVERED);
    }

    @Test
    void inProduction_canAdvanceToReady() {
        long orderId = createOrderWithStatus(OrderStatus.IN_PRODUCTION);
        assertTransitionAllowed(orderId, OrderStatus.READY);
    }

    @Test
    void inProduction_cannotGoBackToConfirmed() {
        long orderId = createOrderWithStatus(OrderStatus.IN_PRODUCTION);
        assertTransitionBlocked(orderId, OrderStatus.CONFIRMED);
    }

    @Test
    void ready_canAdvanceToShipped() {
        long orderId = createOrderWithStatus(OrderStatus.READY);
        assertTransitionAllowed(orderId, OrderStatus.SHIPPED);
    }

    @Test
    void ready_cannotGoBackToInProduction() {
        long orderId = createOrderWithStatus(OrderStatus.READY);
        assertTransitionBlocked(orderId, OrderStatus.IN_PRODUCTION);
    }

    @Test
    void shipped_canAdvanceToDelivered() {
        long orderId = createOrderWithStatus(OrderStatus.SHIPPED);
        assertTransitionAllowed(orderId, OrderStatus.DELIVERED);
    }

    @Test
    void shipped_cannotGoBackToConfirmed() {
        long orderId = createOrderWithStatus(OrderStatus.SHIPPED);
        assertTransitionBlocked(orderId, OrderStatus.CONFIRMED);
    }

    @Test
    void shipped_cannotGoBackToReady() {
        long orderId = createOrderWithStatus(OrderStatus.SHIPPED);
        assertTransitionBlocked(orderId, OrderStatus.READY);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cancellation is always available (except terminal states)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void confirmed_canBeCancelled() {
        long orderId = createOrderWithStatus(OrderStatus.CONFIRMED);
        assertTransitionAllowed(orderId, OrderStatus.CANCELLED);
    }

    @Test
    void inProduction_canBeCancelled() {
        long orderId = createOrderWithStatus(OrderStatus.IN_PRODUCTION);
        assertTransitionAllowed(orderId, OrderStatus.CANCELLED);
    }

    @Test
    void ready_canBeCancelled() {
        long orderId = createOrderWithStatus(OrderStatus.READY);
        assertTransitionAllowed(orderId, OrderStatus.CANCELLED);
    }

    @Test
    void shipped_canBeCancelled() {
        long orderId = createOrderWithStatus(OrderStatus.SHIPPED);
        assertTransitionAllowed(orderId, OrderStatus.CANCELLED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Terminal states are immutable
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void delivered_cannotBeChangedToAnyStatus() {
        long orderId = createOrderWithStatus(OrderStatus.DELIVERED);
        assertTransitionBlocked(orderId, OrderStatus.CONFIRMED);
        assertTransitionBlocked(orderId, OrderStatus.CANCELLED);
        assertTransitionBlocked(orderId, OrderStatus.SHIPPED);
    }

    @Test
    void expired_cannotBeChangedToAnyStatus() {
        long orderId = createOrderWithStatus(OrderStatus.EXPIRED);
        assertTransitionBlocked(orderId, OrderStatus.CONFIRMED);
        assertTransitionBlocked(orderId, OrderStatus.CANCELLED);
        assertTransitionBlocked(orderId, OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void cancelled_cannotBeReactivated() {
        long orderId = createOrderWithStatus(OrderStatus.CANCELLED);
        assertTransitionBlocked(orderId, OrderStatus.CONFIRMED);
        assertTransitionBlocked(orderId, OrderStatus.PENDING_PAYMENT);
        assertTransitionBlocked(orderId, OrderStatus.IN_PRODUCTION);
    }

    @Test
    void cancelled_canBeCancelledAgain_idempotent() {
        // CANCELLED → CANCELLED is a no-op: allowed so double-cancel from race conditions is safe
        long orderId = createOrderWithStatus(OrderStatus.CANCELLED);
        assertTransitionAllowed(orderId, OrderStatus.CANCELLED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NEW status (manually created orders)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void new_canBeConfirmedByAdmin() {
        long orderId = createOrderWithStatus(OrderStatus.NEW);
        assertTransitionAllowed(orderId, OrderStatus.CONFIRMED);
    }

    @Test
    void new_canBeCancelled() {
        long orderId = createOrderWithStatus(OrderStatus.NEW);
        assertTransitionAllowed(orderId, OrderStatus.CANCELLED);
    }

    @Test
    void new_cannotSkipToShipped() {
        long orderId = createOrderWithStatus(OrderStatus.NEW);
        assertTransitionBlocked(orderId, OrderStatus.SHIPPED);
    }
}
