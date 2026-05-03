package com.nurba.java.mapper;

import com.nurba.java.domain.OrderItem;
import com.nurba.java.dto.responce.OrderItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(source = "product.title", target = "productTitle")
    OrderItemResponse toResponse(OrderItem orderItem);
}
