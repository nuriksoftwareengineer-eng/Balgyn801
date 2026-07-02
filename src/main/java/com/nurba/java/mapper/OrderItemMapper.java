package com.nurba.java.mapper;

import com.nurba.java.domain.OrderItem;
import com.nurba.java.dto.responce.OrderItemResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public abstract class OrderItemMapper {

    @Mapping(source = "product.id",                          target = "productId")
    @Mapping(source = "product.title",                        target = "productTitle")
    @Mapping(source = "designGarment.id",                     target = "designGarmentId")
    @Mapping(source = "designGarment.garmentType",            target = "garmentType")
    @Mapping(source = "designGarment.design.name",            target = "designName")
    @Mapping(source = "designGarment.design.slug",            target = "designSlug")
    @Mapping(source = "designGarment.design.collection.catalogGroup.slug", target = "groupSlug")
    @Mapping(source = "designGarment.design.collection.slug", target = "collectionSlug")
    @Mapping(source = "color.id",                             target = "colorId")
    @Mapping(source = "color.hexCode",                        target = "colorHex")
    @Mapping(source = "size.id",                              target = "sizeId")
    @Mapping(source = "currency",                             target = "currency")
    @Mapping(target = "mainImageUrl",                         ignore = true)
    public abstract OrderItemResponse toResponse(OrderItem orderItem);

    /** mainImageUrl can't come from a single MapStruct source: design vs. product. */
    @AfterMapping
    protected void fillMainImageUrl(OrderItem orderItem, @MappingTarget OrderItemResponse response) {
        if (orderItem.getDesignGarment() != null && orderItem.getDesignGarment().getDesign() != null) {
            response.setMainImageUrl(orderItem.getDesignGarment().getDesign().getMainImageUrl());
        } else if (orderItem.getProduct() != null) {
            response.setMainImageUrl(orderItem.getProduct().getImageUrl());
        }
    }
}
