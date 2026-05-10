package com.nurba.java.controller;

import com.nurba.java.api.OrderItemApi;
import com.nurba.java.dto.responce.OrderItemResponse;
import com.nurba.java.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderItemController implements OrderItemApi {

    private final OrderItemService orderItemService;

    @Override
    public List<OrderItemResponse> getAll() {
        return orderItemService.getAll();
    }

    @Override
    public OrderItemResponse getById(Long id) {
        return orderItemService.getById(id);
    }
}
