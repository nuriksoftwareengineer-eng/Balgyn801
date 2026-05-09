package com.nurba.java.service.Impl;

import com.nurba.java.domain.Customer;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.OrderItem;
import com.nurba.java.domain.Product;
import com.nurba.java.dto.request.CreateOrderRequest;
import com.nurba.java.dto.request.OrderItemRequest;
import com.nurba.java.dto.responce.OrderResponse;
import com.nurba.java.mapper.OrderMapper;
import com.nurba.java.repositories.*;
import com.nurba.java.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderMapper orderMapper;


    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        Customer customer = createCustomer(request);

        Order order = new Order();
        order.setCustomer(customer);
        order.setDeliveryType(request.getDeliveryType());
        order.setComment(request.getComment());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setTotalPrice(BigDecimal.ZERO);

        Order savedOrder = orderRepository.save(order);

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (Boolean.FALSE.equals(product.getInStock())) {
                throw new RuntimeException("Product is out of stock");
            }

            int quantity = itemRequest.getQuantity();

            if (quantity <= 0) {
                throw new RuntimeException("Quantity must be greater than 0");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(product.getPrice());

            orderItemRepository.save(orderItem);

            BigDecimal itemTotalPrice = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalPrice = totalPrice.add(itemTotalPrice);
        }

        savedOrder.setTotalPrice(totalPrice);
        savedOrder.setUpdatedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(savedOrder);

        return orderMapper.toResponse(updatedOrder);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + id));

        return orderMapper.toResponse(order);
    }

    private void validateCreateOrderRequest(CreateOrderRequest request) {
        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            throw new RuntimeException("Имя клиента обязательно");
        }

        if (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
            throw new RuntimeException("Телефон клиента обязателен");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Нельзя создать заказ без товаров");
        }

        if (request.getDeliveryType() == null) {
            throw new RuntimeException("Тип доставки обязателен");
        }
    }

    private Customer createCustomer(CreateOrderRequest request) {
        Customer customer = new Customer();
        customer.setName(request.getCustomerName());
        customer.setPhone(request.getCustomerPhone());
        customer.setTelegramUserName(request.getTelegramUsername());
        customer.setCreateAt(LocalDate.now());

        return customerRepository.save(customer);

    }
}
