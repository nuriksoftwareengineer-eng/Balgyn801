package com.nurba.java.controller;

import com.nurba.java.dto.responce.OrderTrackingResponse;
import com.nurba.java.service.OrderTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderTrackingController {

    private final OrderTrackingService orderTrackingService;

    /**
     * Public endpoint — ownership proven by phone number or by being the authenticated owner.
     * Returns 404 for any access failure to prevent order enumeration.
     */
    @GetMapping("/{orderId}/tracking-info")
    public OrderTrackingResponse getTracking(
            @PathVariable Long orderId,
            @RequestParam(value = "phone", required = false) String phone,
            Authentication authentication) {

        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (authenticated && (phone == null || phone.isBlank())) {
            return orderTrackingService.getForUser(orderId, authentication.getName());
        }

        if (phone != null && !phone.isBlank()) {
            return orderTrackingService.getForGuest(orderId, phone);
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phone number required");
    }
}
