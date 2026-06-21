package com.nurba.java.api;

import com.nurba.java.dto.responce.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "Me / Orders", description = "Order history for the authenticated user")
@RequestMapping("/api/v1/me/orders")
public interface MyOrdersApi {

    @Operation(summary = "List own orders, newest first (authenticated)")
    @GetMapping
    List<OrderResponse> getMyOrders(@AuthenticationPrincipal UserDetails userDetails);
}
