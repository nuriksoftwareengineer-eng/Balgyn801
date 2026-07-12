package com.nurba.java.controller;

import com.nurba.java.api.MyOrdersApi;
import com.nurba.java.dto.responce.OrderResponse;
import com.nurba.java.dto.responce.OrderTrackingResponse;
import com.nurba.java.service.OrderService;
import com.nurba.java.service.OrderTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MyOrdersController implements MyOrdersApi {

    private final OrderService orderService;
    private final OrderTrackingService orderTrackingService;

    @Override
    public List<OrderResponse> getMyOrders(UserDetails userDetails) {
        return orderService.getMyOrders(userDetails.getUsername());
    }

    @Override
    public OrderTrackingResponse getMyOrderTracking(Long orderId, UserDetails userDetails) {
        return orderTrackingService.getForUser(orderId, userDetails.getUsername());
    }
}
