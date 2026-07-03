package com.nurba.java.service.Impl;

import com.nurba.java.domain.GarmentProfile;
import com.nurba.java.dto.request.CreateGarmentProfileRequest;
import com.nurba.java.dto.responce.GarmentProfileResponse;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.GarmentProfileMapper;
import com.nurba.java.repositories.GarmentProfileRepository;
import com.nurba.java.service.GarmentProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GarmentProfileServiceImpl implements GarmentProfileService {

    private final GarmentProfileRepository repository;
    private final GarmentProfileMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<GarmentProfileResponse> listAll() {
        return repository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GarmentProfileResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public GarmentProfileResponse create(CreateGarmentProfileRequest request) {
        if (repository.existsByNameIgnoreCase(request.name().trim())) {
            throw new BusinessRuleException("Тип одежды с таким именем уже существует: " + request.name());
        }
        GarmentProfile entity = mapper.toEntity(request);
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public GarmentProfileResponse update(Long id, CreateGarmentProfileRequest request) {
        GarmentProfile entity = findOrThrow(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new BusinessRuleException("Тип одежды с таким именем уже существует: " + request.name());
        }
        mapper.updateEntity(request, entity);
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        try {
            repository.deleteById(id);
            repository.flush();
        } catch (Exception e) {
            throw new BusinessRuleException(
                    "Невозможно удалить тип одежды — он используется в вариантах дизайна");
        }
    }

    private GarmentProfile findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Тип одежды не найден: id=" + id));
    }
}
