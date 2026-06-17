package com.nurba.java.service.Impl;

import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Design;
import com.nurba.java.dto.request.CreateDesignRequest;
import com.nurba.java.dto.responce.DesignResponse;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.DesignMapper;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignServiceImpl implements DesignService {

    private final DesignRepository repository;
    private final CollectionRepository collectionRepository;
    private final DesignMapper mapper;

    @Override
    public List<DesignResponse> getAll(Long collectionId) {
        List<Design> list = (collectionId != null)
                ? repository.findByCollection_Id(collectionId)
                : repository.findAll();
        return list.stream().map(mapper::toResponse).toList();
    }

    @Override
    public DesignResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public DesignResponse create(CreateDesignRequest request) {
        if (repository.existsBySlug(request.getSlug())) {
            throw new BusinessRuleException("Slug already in use: " + request.getSlug());
        }
        Collection collection = collectionRepository.findById(request.getCollectionId())
                .orElseThrow(() -> new NotFoundException("Collection not found: " + request.getCollectionId()));
        Design entity = mapper.toEntity(request);
        entity.setCollection(collection);
        entity.setCreatedAt(LocalDateTime.now());
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public DesignResponse update(Long id, CreateDesignRequest request) {
        Design entity = findOrThrow(id);
        Collection collection = collectionRepository.findById(request.getCollectionId())
                .orElseThrow(() -> new NotFoundException("Collection not found: " + request.getCollectionId()));
        entity.setName(request.getName());
        entity.setSlug(request.getSlug());
        entity.setDescription(request.getDescription());
        entity.setMainImageUrl(request.getMainImageUrl());
        entity.setGallery(request.getGallery() != null ? request.getGallery() : new ArrayList<>());
        entity.setCollection(collection);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private Design findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Design not found: " + id));
    }
}
