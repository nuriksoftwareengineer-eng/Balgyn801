package com.nurba.java.service;

import com.nurba.java.domain.ParcelTracking;

import java.util.List;

public interface ParcelTrackingService {

    /**
     * Register a tracking number for an order.
     * If already registered, re-fetches immediately.
     */
    ParcelTracking register(Long orderId, String carrier, String trackingNumber);

    /** Returns all tracking entries for an order. */
    List<ParcelTracking> getByOrderId(Long orderId);

    /**
     * Fetch fresh data from the provider and persist.
     * Safe to call on a schedule — ignores provider errors, keeps last-known-good.
     */
    ParcelTracking refresh(Long trackingId);
}
