package com.nurba.java.mapper;

import com.nurba.java.domain.DeliveryAddress;
import com.nurba.java.dto.request.DeliveryAddressRequest;
import com.nurba.java.dto.responce.DeliveryAddressResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeliveryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    DeliveryAddress toEntity(DeliveryAddressRequest request);

    DeliveryAddressResponse toResponse(DeliveryAddress address);
}
