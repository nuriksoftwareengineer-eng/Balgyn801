package com.nurba.java.service;

import com.nurba.java.dto.request.CreateOrderRequest;
import com.nurba.java.dto.responce.OrderResponse;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(Long id);
}
