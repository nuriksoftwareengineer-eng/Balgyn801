package com.nurba.java.mapper;

import com.nurba.java.domain.CdekShipment;
import com.nurba.java.dto.responce.CdekShipmentResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CdekMapper {

    CdekShipmentResponse toResponse(CdekShipment cdekShipment);
}
