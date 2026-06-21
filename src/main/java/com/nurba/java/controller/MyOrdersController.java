package com.nurba.java.controller;

import com.nurba.java.api.MyOrdersApi;
import com.nurba.java.dto.responce.OrderResponse;
import com.nurba.java.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MyOrdersController implements MyOrdersApi {

    private final OrderService orderService;

    @Override
    public List<OrderResponse> getMyOrders(UserDetails userDetails) {
        return orderService.getMyOrders(userDetails.getUsername());
    }
}
