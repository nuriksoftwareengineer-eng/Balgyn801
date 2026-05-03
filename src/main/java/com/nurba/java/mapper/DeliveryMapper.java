package com.nurba.java.mapper;

import com.nurba.java.domain.DeliveryAddress;
import com.nurba.java.dto.request.DeliveryAddressRequest;
import com.nurba.java.dto.responce.DeliveryAddressResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeliveryMapper {

    DeliveryAddress toEntity(DeliveryAddressRequest request);

    DeliveryAddressResponse toResponse(DeliveryAddress address);
}
