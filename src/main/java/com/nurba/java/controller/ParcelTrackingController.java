package com.nurba.java.controller;

import com.nurba.java.domain.ParcelTracking;
import com.nurba.java.service.ParcelTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ParcelTrackingController {

    private final ParcelTrackingService service;

    /**
     * Public tracking lookup. Access requires proof of ownership: either the authenticated owner of
     * the order, or a matching {@code phone} (the number the order was placed with). Returns an empty
     * list otherwise — never reveals whether the order exists (prevents IDOR enumeration by orderId).
     */
    @GetMapping("/orders/{orderId}/tracking")
    public List<ParcelTracking> getTracking(
            @PathVariable Long orderId,
            @RequestParam(value = "phone", required = false) String phone,
            Authentication authentication) {
        String email = (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken))
                ? authentication.getName() : null;
        return service.getForRequester(orderId, phone, email);
    }

    /** Admin: register/update tracking for an order. */
    @PostMapping("/admin/orders/{orderId}/tracking")
    @PreAuthorize("hasRole('ADMIN')")
    public ParcelTracking register(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body) {
        return service.register(orderId, body.get("carrier"), body.get("trackingNumber"));
    }

    /** Admin: manually trigger a refresh. */
    @PostMapping("/admin/tracking/{trackingId}/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParcelTracking> refresh(@PathVariable Long trackingId) {
        return ResponseEntity.ok(service.refresh(trackingId));
    }
}
