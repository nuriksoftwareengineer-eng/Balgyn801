package com.nurba.java.service;

import com.nurba.java.domain.ParcelTracking;

import java.util.List;

public interface ParcelTrackingService {

    /**
     * Register a tracking number for an order.
     * If already registered, re-fetches immediately.
     */
    ParcelTracking register(Long orderId, String carrier, String trackingNumber);

    /** Returns all tracking entries for an order. Internal/admin use — no access control. */
    List<ParcelTracking> getByOrderId(Long orderId);

    /**
     * Ownership-guarded lookup for the PUBLIC endpoint. Returns tracking only when the requester
     * proves access: either they are the authenticated owner of the order, or they supply the phone
     * number the order was placed with. Returns an empty list otherwise — never reveals whether the
     * order exists, preventing IDOR enumeration by sequential orderId.
     */
    List<ParcelTracking> getForRequester(Long orderId, String phone, String requesterEmail);

    /**
     * Fetch fresh data from the provider and persist.
     * Safe to call on a schedule — ignores provider errors, keeps last-known-good.
     */
    ParcelTracking refresh(Long trackingId);
}
