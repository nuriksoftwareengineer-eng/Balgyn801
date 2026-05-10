package com.nurba.java.controller;

import com.nurba.java.api.OrderApi;
import com.nurba.java.dto.request.CreateOrderRequest;
import com.nurba.java.dto.responce.OrderResponse;
import com.nurba.java.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderService orderService;

    @Override
    public List<OrderResponse> getAll() {
        return orderService.getAll();
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        return orderService.getOrderById(id);
    }
}
