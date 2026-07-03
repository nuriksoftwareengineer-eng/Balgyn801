package com.nurba.java.repositories;

import com.nurba.java.domain.Order;
import com.nurba.java.domain.Payment;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    /**
     * Pessimistic write lock version — use in capture flow to prevent double-capture
     * when two concurrent requests both see PENDING and attempt to call PayPal.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.providerPaymentId = :id")
    Optional<Payment> findByProviderPaymentIdForUpdate(@Param("id") String id);

    /** Idempotency check: is there already an active payment for this order+provider? */
    Optional<Payment> findByOrderAndProviderAndStatus(Order order, PaymentProvider provider, PaymentStatus status);

    /** All payments for an order in the given status — used to bulk-cancel on order expiry/cancellation. */
    List<Payment> findByOrderAndStatus(Order order, PaymentStatus status);

    // ── Admin list queries ────────────────────────────────────────────────────

    List<Payment> findAllByOrderByCreatedAtDesc();
    List<Payment> findByProviderOrderByCreatedAtDesc(PaymentProvider provider);
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
    List<Payment> findByProviderAndStatusOrderByCreatedAtDesc(PaymentProvider provider, PaymentStatus status);

    @Query("""
            SELECT p FROM Payment p
            WHERE (:provider IS NULL OR p.provider = :provider)
              AND (:status   IS NULL OR p.status   = :status)
              AND (:q IS NULL OR :q = ''
                   OR CAST(p.id AS string) LIKE CONCAT('%', :q, '%')
                   OR LOWER(p.providerPaymentId) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY p.createdAt DESC
            """)
    Page<Payment> searchAdmin(
            @Param("provider") PaymentProvider provider,
            @Param("status") PaymentStatus status,
            @Param("q") String q,
            Pageable pageable);
}
