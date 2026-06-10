package com.nurba.java.mapper;

import com.nurba.java.domain.OrderItem;
import com.nurba.java.dto.responce.OrderItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(source = "product.title",           target = "productTitle")
    @Mapping(source = "designGarment.id",         target = "designGarmentId")
    @Mapping(source = "designGarment.garmentType",target = "garmentType")
    @Mapping(source = "designGarment.design.name",target = "designName")
    @Mapping(source = "color.id",                 target = "colorId")
    @Mapping(source = "color.hexCode",            target = "colorHex")
    @Mapping(source = "size.id",                  target = "sizeId")
    @Mapping(source = "currency",                 target = "currency")
    OrderItemResponse toResponse(OrderItem orderItem);
}
