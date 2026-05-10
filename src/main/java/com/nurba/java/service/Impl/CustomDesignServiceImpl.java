package com.nurba.java.service.Impl;

import com.nurba.java.domain.CustomDesign;
import com.nurba.java.domain.Customer;
import com.nurba.java.dto.request.CreateCustomDesignRequest;
import com.nurba.java.dto.responce.CustomDesignResponse;
import com.nurba.java.enums.CustomDesignStatus;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.CustomDesignMapper;
import com.nurba.java.repositories.CustomerDesignRepository;
import com.nurba.java.repositories.CustomerRepository;
import com.nurba.java.service.CustomDesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomDesignServiceImpl implements CustomDesignService {

    private final CustomerDesignRepository customerDesignRepository;
    private final CustomerRepository customerRepository;
    private final CustomDesignMapper customDesignMapper;

    @Override
    public CustomDesignResponse create(CreateCustomDesignRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Клиент не найден"));

        CustomDesign customDesign = customDesignMapper.toEntity(request);
        customDesign.setCustomer(customer);
        customDesign.setStatus(CustomDesignStatus.PENDING);
        customDesign.setCreatedAt(LocalDateTime.now());

        return customDesignMapper.toResponse(customerDesignRepository.save(customDesign));
    }

    @Override
    public CustomDesignResponse getById(Long id) {
        return customDesignMapper.toResponse(customerDesignRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Кастомный дизайн не найден")));
    }

    @Override
    public List<CustomDesignResponse> getAll() {
        return customerDesignRepository.findAll()
                .stream()
                .map(customDesignMapper::toResponse)
                .toList();
    }
}
