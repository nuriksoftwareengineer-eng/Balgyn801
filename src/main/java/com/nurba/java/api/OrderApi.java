package com.nurba.java.api;

import com.nurba.java.dto.request.CreateOrderRequest;
import com.nurba.java.dto.responce.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Order", description = "Заказы")
@RequestMapping("/api/v1/order")
public interface OrderApi {

    @Operation(summary = "Список заказов")
    @GetMapping
    List<OrderResponse> getAll();

    @Operation(summary = "Оформить заказ")
    @PostMapping
    OrderResponse createOrder(@RequestBody CreateOrderRequest request);

    @Operation(summary = "Заказ по ID")
    @GetMapping("/{id}")
    OrderResponse getOrderById(@PathVariable Long id);
}
