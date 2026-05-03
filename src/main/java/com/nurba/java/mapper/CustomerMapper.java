package com.nurba.java.mapper;

import com.nurba.java.domain.Customer;
import com.nurba.java.dto.request.CreateOrderRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    Customer toEntity(CreateOrderRequest request);
}
