package com.nurba.java.repositories;

import com.nurba.java.domain.Order;
import com.nurba.java.domain.Payment;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    /** Idempotency check: is there already an active payment for this order+provider? */
    Optional<Payment> findByOrderAndProviderAndStatus(Order order, PaymentProvider provider, PaymentStatus status);

    /** All payments for an order in the given status — used to bulk-cancel on order expiry/cancellation. */
    List<Payment> findByOrderAndStatus(Order order, PaymentStatus status);
}
