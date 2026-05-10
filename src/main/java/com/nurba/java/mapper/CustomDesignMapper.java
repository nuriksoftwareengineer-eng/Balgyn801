package com.nurba.java.mapper;

import com.nurba.java.domain.CustomDesign;
import com.nurba.java.dto.request.CreateCustomDesignRequest;
import com.nurba.java.dto.responce.CustomDesignResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomDesignMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    CustomDesign toEntity(CreateCustomDesignRequest request);

    @Mapping(source = "customer.id", target = "customerId")
    CustomDesignResponse toResponse(CustomDesign customDesign);
}