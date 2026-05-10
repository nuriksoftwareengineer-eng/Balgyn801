package com.nurba.java.api;

import com.nurba.java.dto.responce.OrderItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "Order Item", description = "Позиции заказа")
@RequestMapping("/api/v1/order-item")
public interface OrderItemApi {

    @Operation(summary = "Список позиций заказа")
    @GetMapping
    List<OrderItemResponse> getAll();

    @Operation(summary = "Позиция заказа по ID")
    @GetMapping("/{id}")
    OrderItemResponse getById(@PathVariable Long id);
}
