package com.nurba.java.service.Impl;

import com.nurba.java.component.DesignReadinessService;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.DesignGarmentPrice;
import com.nurba.java.dto.request.CreateDesignGarmentPriceRequest;
import com.nurba.java.dto.responce.DesignGarmentPriceResponse;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.DesignGarmentPriceMapper;
import com.nurba.java.repositories.DesignGarmentPriceRepository;
import com.nurba.java.repositories.DesignGarmentRepository;
import com.nurba.java.service.DesignGarmentPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DesignGarmentPriceServiceImpl implements DesignGarmentPriceService {

    private final DesignGarmentPriceRepository repository;
    private final DesignGarmentRepository garmentRepository;
    private final DesignGarmentPriceMapper mapper;
    private final DesignReadinessService readinessService;

    @Override
    @Transactional(readOnly = true)
    public List<DesignGarmentPriceResponse> getByGarment(Long designGarmentId) {
        return repository.findByDesignGarment_Id(designGarmentId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public DesignGarmentPriceResponse upsert(CreateDesignGarmentPriceRequest request) {
        DesignGarment garment = garmentRepository.findById(request.getDesignGarmentId())
                .orElseThrow(() -> new NotFoundException("Design garment not found: " + request.getDesignGarmentId()));

        Optional<DesignGarmentPrice> existing = repository.findByDesignGarment_IdAndCurrency(
                request.getDesignGarmentId(), request.getCurrency());

        DesignGarmentPrice entity = existing.orElseGet(() -> {
            DesignGarmentPrice p = mapper.toEntity(request);
            p.setDesignGarment(garment);
            return p;
        });
        entity.setAmount(request.getAmount());
        DesignGarmentPriceResponse response = mapper.toResponse(repository.save(entity));
        readinessService.recompute(garment.getDesign().getId());
        return response;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DesignGarmentPrice price = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Price not found: " + id));
        Long designId = price.getDesignGarment().getDesign().getId();
        repository.deleteById(id);
        readinessService.recompute(designId);
    }
}
