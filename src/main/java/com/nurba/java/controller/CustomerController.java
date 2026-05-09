package com.nurba.java.controller;

import com.nurba.java.api.CustomerApi;
import com.nurba.java.dto.request.CustomerRequest;
import com.nurba.java.dto.responce.CustomerResponse;
import com.nurba.java.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CustomerController implements CustomerApi {

    private final CustomerService customerService;

    @Override
    public List<CustomerResponse> getAll() {
        return customerService.getAll();
    }

    @Override
    public CustomerResponse getById(Long id) {
        return customerService.getById(id);
    }

    @Override
    public CustomerResponse create(@RequestBody CustomerRequest customerRequest) {
        return customerService.create(customerRequest);
    }

    @Override
    public CustomerResponse update(@RequestBody CustomerRequest customerRequest) {
        return customerService.update(customerRequest);
    }

    @Override
    public void delete(Long id) {
        customerService.delete(id);
    }
}
