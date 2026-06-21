package com.nurba.java.repositories;

import com.nurba.java.domain.ProcessedWebhookEvent;
import com.nurba.java.enums.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, Long> {

    boolean existsByProviderAndEventId(PaymentProvider provider, String eventId);
}
