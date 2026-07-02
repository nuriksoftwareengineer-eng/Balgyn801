package com.nurba.java.controller;

import com.nurba.java.api.OrderApi;
import com.nurba.java.dto.request.CreateOrderRequest;
import com.nurba.java.dto.request.UpdateOrderStatusRequest;
import com.nurba.java.dto.responce.OrderResponse;
import com.nurba.java.dto.responce.PageResponse;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.mapper.OrderMapper;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    public List<OrderResponse> getAll() {
        return orderService.getAll();
    }

    /** Paginated + searchable admin order list. */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<OrderResponse> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        var pageable = PageRequest.of(page, size);
        var excludeStatuses = List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.EXPIRED);
        var result = orderRepository.searchAdmin(excludeStatuses, q.isBlank() ? null : q, pageable);
        return PageResponse.of(result.map(orderMapper::toResponse));
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        return orderService.getOrderById(id);
    }

    @Override
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        return orderService.updateOrderStatus(id, request);
    }
}
