package com.nurba.java.mapper;

import com.nurba.java.domain.Customer;
import com.nurba.java.dto.request.CustomerRequest;
import com.nurba.java.dto.responce.CustomerResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    Customer toRequest(CustomerRequest customerRequest);
    CustomerResponse toResponse(Customer customer);
    List<CustomerResponse> toListResponse(List<Customer> customers);
}
