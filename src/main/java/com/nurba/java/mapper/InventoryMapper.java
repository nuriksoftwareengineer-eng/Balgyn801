package com.nurba.java.mapper;

import com.nurba.java.domain.Inventory;
import com.nurba.java.dto.request.SetInventoryRequest;
import com.nurba.java.dto.responce.InventoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "designGarment", ignore = true)
    @Mapping(target = "color", ignore = true)
    @Mapping(target = "size", ignore = true)
    Inventory toEntity(SetInventoryRequest request);

    @Mapping(source = "designGarment.id", target = "designGarmentId")
    @Mapping(source = "color.id", target = "colorId")
    @Mapping(source = "color.name", target = "colorName")
    @Mapping(source = "size.id", target = "sizeId")
    @Mapping(source = "size.label", target = "sizeLabel")
    InventoryResponse toResponse(Inventory entity);
}
