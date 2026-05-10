package com.nurba.java.service;

import com.nurba.java.dto.responce.OrderItemResponse;

import java.util.List;

public interface OrderItemService {

    OrderItemResponse getById(Long id);
    List<OrderItemResponse> getAll();
}
