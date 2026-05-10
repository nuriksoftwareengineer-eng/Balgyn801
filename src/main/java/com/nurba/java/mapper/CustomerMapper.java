package com.nurba.java.mapper;

import com.nurba.java.domain.Customer;
import com.nurba.java.dto.request.CustomerRequest;
import com.nurba.java.dto.responce.CustomerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(source = "telegramUsername", target = "telegramUserName")
    Customer toRequest(CustomerRequest customerRequest);

    @Mapping(source = "telegramUserName", target = "telegramUsername")
    CustomerResponse toResponse(Customer customer);

    List<CustomerResponse> toListResponse(List<Customer> customers);

    default LocalDate map(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    default LocalDateTime map(LocalDate value) {
        return value == null ? null : value.atStartOfDay();
    }
}
