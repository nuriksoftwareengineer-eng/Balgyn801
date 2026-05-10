package com.nurba.java.mapper;

import com.nurba.java.domain.Order;
import com.nurba.java.dto.responce.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class, DeliveryMapper.class, CdekMapper.class})
public interface OrderMapper {

    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "customer.phone", target = "customerPhone")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(source = "deliveryAddress", target = "address")
    @Mapping(target = "cdekShipment", ignore = true)
    OrderResponse toResponse(Order order);
}