package com.nurba.java.mapper;

import com.nurba.java.domain.CdekShipment;
import com.nurba.java.dto.responce.CdekShipmentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CdekMapper {

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(target = "mock", ignore = true) // выставляется в сервисе по активному провайдеру
    CdekShipmentResponse toResponse(CdekShipment cdekShipment);
}
