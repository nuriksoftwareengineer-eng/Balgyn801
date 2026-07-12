package com.nurba.java.service;

import com.nurba.java.dto.responce.OrderTrackingResponse;

public interface OrderTrackingService {
    /** Public tracking — ownership proven by phone number. */
    OrderTrackingResponse getForGuest(Long orderId, String phone);
    /** Authenticated tracking — ownership proven by user email. */
    OrderTrackingResponse getForUser(Long orderId, String userEmail);
}
