package com.nurba.java.service.Impl;

import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.dto.request.CreateCatalogGroupRequest;
import com.nurba.java.dto.responce.CatalogGroupResponse;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.CatalogGroupMapper;
import com.nurba.java.repositories.CatalogGroupRepository;
import com.nurba.java.service.CatalogGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogGroupServiceImpl implements CatalogGroupService {

    private final CatalogGroupRepository repository;
    private final CatalogGroupMapper mapper;

    @Override
    public List<CatalogGroupResponse> getAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    public CatalogGroupResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public CatalogGroupResponse create(CreateCatalogGroupRequest request) {
        if (repository.existsBySlug(request.getSlug())) {
            throw new BusinessRuleException("Slug already in use: " + request.getSlug());
        }
        CatalogGroup entity = mapper.toEntity(request);
        entity.setCreatedAt(LocalDateTime.now());
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public CatalogGroupResponse update(Long id, CreateCatalogGroupRequest request) {
        CatalogGroup entity = findOrThrow(id);
        entity.setName(request.getName());
        entity.setSlug(request.getSlug());
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private CatalogGroup findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Catalog group not found: " + id));
    }
}
