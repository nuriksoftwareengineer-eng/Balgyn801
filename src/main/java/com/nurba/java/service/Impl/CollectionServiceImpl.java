package com.nurba.java.service.Impl;

import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Design;
import com.nurba.java.dto.request.CreateCollectionRequest;
import com.nurba.java.dto.responce.CollectionResponse;
import com.nurba.java.enums.DesignStatus;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.CollectionMapper;
import com.nurba.java.repositories.CatalogGroupRepository;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final CollectionRepository repository;
    private final CatalogGroupRepository groupRepository;
    private final DesignRepository designRepository;
    private final CollectionMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CollectionResponse> getAll(Long groupId) {
        List<Collection> list = (groupId != null)
                ? repository.findByCatalogGroup_Id(groupId)
                : repository.findAll();
        return list.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public CollectionResponse create(CreateCollectionRequest request) {
        if (repository.existsBySlug(request.getSlug())) {
            throw new BusinessRuleException("Slug already in use: " + request.getSlug());
        }
        CatalogGroup group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new NotFoundException("Catalog group not found: " + request.getGroupId()));
        Collection entity = mapper.toEntity(request);
        entity.setCatalogGroup(group);
        entity.setCreatedAt(LocalDateTime.now());
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public CollectionResponse update(Long id, CreateCollectionRequest request) {
        Collection entity = findOrThrow(id);
        CatalogGroup group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new NotFoundException("Catalog group not found: " + request.getGroupId()));
        entity.setName(request.getName());
        entity.setSlug(request.getSlug());
        entity.setDescription(request.getDescription());
        entity.setCoverImageUrl(request.getCoverImageUrl());
        entity.setBannerImageUrl(request.getBannerImageUrl());
        entity.setCatalogGroup(group);
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        List<Design> designs = designRepository.findByCollection_Id(id);

        // Block on non-archived designs — user must archive or delete them first.
        List<String> blocking = designs.stream()
                .filter(d -> d.getStatus() != DesignStatus.ARCHIVED)
                .map(Design::getName)
                .toList();
        if (!blocking.isEmpty()) {
            throw new BusinessRuleException(
                    "Нельзя удалить коллекцию: сначала архивируйте или удалите дизайны: "
                    + String.join(", ", blocking));
        }

        // Archived designs tied to orders cannot be physically deleted.
        // Detach them from this collection so it can be removed.
        if (!designs.isEmpty()) {
            designs.forEach(d -> d.setCollection(null));
            designRepository.saveAll(designs);
            designRepository.flush();
        }

        repository.deleteById(id);
    }

    private Collection findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Collection not found: " + id));
    }
}
