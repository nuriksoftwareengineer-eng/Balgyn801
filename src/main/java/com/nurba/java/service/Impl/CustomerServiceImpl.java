package com.nurba.java.service.Impl;

import com.nurba.java.dto.request.CustomerRequest;
import com.nurba.java.dto.responce.CustomerResponse;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.CustomerMapper;
import com.nurba.java.repositories.CustomerRepository;
import com.nurba.java.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public List<CustomerResponse> getAll() {
        return customerMapper.toListResponse(customerRepository.findAll());
    }

    @Override
    public CustomerResponse getById(Long id) {
        return customerMapper.toResponse(customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Клиент не найден")));
    }

    @Override
    public CustomerResponse create(CustomerRequest customerRequest) {
        return customerMapper.toResponse(customerRepository.save(customerMapper.toRequest(customerRequest)));
    }

    @Override
    public CustomerResponse update(CustomerRequest customerRequest) {
        return customerMapper.toResponse(customerRepository.save(customerMapper.toRequest(customerRequest)));
    }

    @Override
    public void delete(Long id) {
        customerRepository.deleteById(id);
    }
}
