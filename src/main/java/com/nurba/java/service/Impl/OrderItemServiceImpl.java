package com.nurba.java.service.Impl;

import com.nurba.java.dto.responce.OrderItemResponse;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.OrderItemMapper;
import com.nurba.java.repositories.OrderItemRepository;
import com.nurba.java.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderItemMapper orderItemMapper;

    @Override
    public OrderItemResponse getById(Long id) {
        return orderItemMapper.toResponse(orderItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Позиция заказа не найдена")));
    }

    @Override
    public List<OrderItemResponse> getAll() {
        return orderItemRepository.findAll()
                .stream()
                .map(orderItemMapper::toResponse)
                .toList();
    }
}
