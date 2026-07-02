package com.nurba.java.controller;

import com.nurba.java.api.CustomerApi;
import com.nurba.java.dto.request.CustomerRequest;
import com.nurba.java.dto.responce.CustomerResponse;
import com.nurba.java.dto.responce.PageResponse;
import com.nurba.java.mapper.CustomerMapper;
import com.nurba.java.repositories.CustomerRepository;
import com.nurba.java.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CustomerController implements CustomerApi {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

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

    /** Paginated search — separate from the legacy getAll() to preserve API compatibility. */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<CustomerResponse> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("id").descending());
        var result = q.isBlank()
                ? customerRepository.findAll(pageable)
                : customerRepository.search(q, pageable);
        return PageResponse.of(result.map(customerMapper::toResponse));
    }
}
