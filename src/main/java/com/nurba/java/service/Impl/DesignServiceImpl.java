package com.nurba.java.service.Impl;

import com.nurba.java.component.DesignReadinessService;
import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Design;
import com.nurba.java.dto.request.CreateDesignRequest;
import com.nurba.java.dto.responce.DesignResponse;
import com.nurba.java.enums.DesignStatus;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.exception.PublicationValidationException;
import com.nurba.java.mapper.DesignMapper;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignServiceImpl implements DesignService {

    private final DesignRepository repository;
    private final CollectionRepository collectionRepository;
    private final DesignMapper mapper;
    private final DesignReadinessService readinessService;

    @Override
    @Transactional(readOnly = true)
    public List<DesignResponse> getAll(Long collectionId) {
        List<Design> list = (collectionId != null)
                ? repository.findByCollectionIdWithGarments(collectionId)
                : repository.findAllWithGarments();
        return list.stream().map(d -> {
            DesignResponse r = mapper.toResponse(d);
            int activeCount = (int) d.getGarments().stream()
                    .filter(g -> Boolean.TRUE.equals(g.getActive()))
                    .count();
            r.setActiveGarmentCount(activeCount);
            return r;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DesignResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public DesignResponse create(CreateDesignRequest request) {
        if (repository.existsBySlug(request.getSlug())) {
            throw new BusinessRuleException("Slug already in use: " + request.getSlug());
        }
        Collection collection = collectionRepository.findById(request.getCollectionId())
                .orElseThrow(() -> new NotFoundException("Collection not found: " + request.getCollectionId()));
        Design entity = mapper.toEntity(request);
        entity.setCollection(collection);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setStatus(DesignStatus.DRAFT);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
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
        DesignResponse response = mapper.toResponse(repository.save(entity));
        // Re-evaluate readiness after update (may flip DRAFT ↔ READY based on new image)
        readinessService.recompute(id);
        return response;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public DesignResponse publish(Long id) {
        Design entity = findOrThrow(id);
        if (entity.getStatus() == DesignStatus.ARCHIVED) {
            throw new BusinessRuleException("Archived designs cannot be published directly. Restore first.");
        }
        if (entity.getStatus() == DesignStatus.PUBLISHED) {
            return mapper.toResponse(entity);
        }
        List<String> errors = readinessService.validationErrors(entity);
        if (!errors.isEmpty()) {
            throw new PublicationValidationException(errors);
        }
        entity.setStatus(DesignStatus.PUBLISHED);
        // Record first publish time; never overwrite if already set
        if (entity.getPublishedAt() == null) {
            entity.setPublishedAt(LocalDateTime.now());
        }
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public DesignResponse unpublish(Long id) {
        Design entity = findOrThrow(id);
        if (entity.getStatus() != DesignStatus.PUBLISHED) {
            return mapper.toResponse(entity);
        }
        // Transition to READY if requirements still met, otherwise DRAFT
        boolean ready = readinessService.validationErrors(entity).isEmpty();
        entity.setStatus(ready ? DesignStatus.READY : DesignStatus.DRAFT);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public DesignResponse archive(Long id) {
        Design entity = findOrThrow(id);
        if (entity.getStatus() == DesignStatus.ARCHIVED) {
            return mapper.toResponse(entity);
        }
        entity.setStatus(DesignStatus.ARCHIVED);
        entity.setArchivedAt(LocalDateTime.now());
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public DesignResponse restore(Long id) {
        Design entity = findOrThrow(id);
        if (entity.getStatus() != DesignStatus.ARCHIVED) {
            throw new BusinessRuleException("Only archived designs can be restored. Current status: " + entity.getStatus());
        }
        entity.setStatus(DesignStatus.DRAFT);
        entity.setArchivedAt(null);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public void recomputeStatus(Long designId) {
        readinessService.recompute(designId);
    }

    private Design findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Design not found: " + id));
    }
}
