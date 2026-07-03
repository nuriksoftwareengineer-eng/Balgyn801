package com.nurba.java.service.Impl;

import com.nurba.java.domain.Order;
import com.nurba.java.domain.ParcelTracking;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.repositories.ParcelTrackingRepository;
import com.nurba.java.service.ParcelTrackingService;
import com.nurba.java.tracking.ParcelTrackingProvider;
import com.nurba.java.tracking.TrackingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParcelTrackingServiceImpl implements ParcelTrackingService {

    private final ParcelTrackingRepository repository;
    private final OrderRepository orderRepository;

    /**
     * Spring will inject all registered ParcelTrackingProvider beans.
     * Currently: SeventeenTrackProvider. Future providers just need @Component.
     */
    private final List<ParcelTrackingProvider> providers;

    @Override
    @Transactional
    public ParcelTracking register(Long orderId, String carrier, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        ParcelTracking tracking = repository
                .findByOrder_IdAndCarrierAndTrackingNumber(orderId, carrier, trackingNumber)
                .orElseGet(() -> {
                    ParcelTracking t = new ParcelTracking();
                    t.setOrder(order);
                    t.setCarrier(carrier);
                    t.setTrackingNumber(trackingNumber);
                    t.setCreatedAt(LocalDateTime.now());
                    t.setUpdatedAt(LocalDateTime.now());
                    return t;
                });

        tracking.setProvider(pickProvider().providerName());
        return doFetch(tracking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParcelTracking> getByOrderId(Long orderId) {
        return repository.findByOrder_Id(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParcelTracking> getForRequester(Long orderId, String phone, String requesterEmail) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return List.of();   // non-enumerable: do not reveal whether the order exists
        }
        boolean owner = requesterEmail != null
                && order.getAppUser() != null
                && requesterEmail.equalsIgnoreCase(order.getAppUser().getEmail());
        boolean phoneOk = order.getCustomer() != null
                && phoneMatches(order.getCustomer().getPhone(), phone);
        if (!owner && !phoneOk) {
            log.warn("[Tracking] Access denied for orderId={} (no ownership / phone mismatch)", orderId);
            return List.of();
        }
        return repository.findByOrder_Id(orderId);
    }

    /** Compares the last 10 significant digits; requires ≥7 provided digits to resist guessing. */
    private static boolean phoneMatches(String stored, String provided) {
        String a = digitsOnly(stored);
        String b = digitsOnly(provided);
        if (a.length() < 7 || b.length() < 7) {
            return false;
        }
        return tail(a).equals(tail(b));
    }

    private static String digitsOnly(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private static String tail(String s) {
        return s.length() <= 10 ? s : s.substring(s.length() - 10);
    }

    @Override
    @Transactional
    public ParcelTracking refresh(Long trackingId) {
        ParcelTracking tracking = repository.findById(trackingId)
                .orElseThrow(() -> new NotFoundException("Tracking not found: " + trackingId));
        return doFetch(tracking);
    }

    private ParcelTracking doFetch(ParcelTracking tracking) {
        ParcelTrackingProvider provider = pickProvider();
        try {
            TrackingResult result = provider.fetch(tracking.getTrackingNumber(), tracking.getCarrier());
            if (result != null) {
                tracking.setLastStatus(result.getLastStatus());
                tracking.setStatusDetail(result.getStatusDetail());
                tracking.setEvents(result.getEvents());
                tracking.setErrorMessage(null);
            }
        } catch (Exception e) {
            log.warn("Tracking fetch error for {}: {}", tracking.getTrackingNumber(), e.getMessage());
            tracking.setErrorMessage(e.getMessage());
        }
        tracking.setUpdatedAt(LocalDateTime.now());
        return repository.save(tracking);
    }

    private ParcelTrackingProvider pickProvider() {
        return providers.stream()
                .filter(ParcelTrackingProvider::isAvailable)
                .findFirst()
                .orElse(providers.get(0)); // fall back to first (stub mode)
    }
}
