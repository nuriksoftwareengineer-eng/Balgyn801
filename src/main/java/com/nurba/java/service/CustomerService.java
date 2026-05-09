package com.nurba.java.service;

import com.nurba.java.dto.request.CustomerRequest;
import com.nurba.java.dto.responce.CustomerResponse;

import java.util.List;

public interface CustomerService {
    List<CustomerResponse> getAll();
    CustomerResponse getById(Long id);
    CustomerResponse create(CustomerRequest customerRequest);
    CustomerResponse update(CustomerRequest customerRequest);
    void delete(Long id);

}
