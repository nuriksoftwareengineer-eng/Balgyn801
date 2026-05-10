package com.nurba.java.service;

import com.nurba.java.dto.request.CreateOrderRequest;
import com.nurba.java.dto.responce.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(Long id);

    List<OrderResponse> getAll();
}
