package com.nurba.java.service.Impl;

import com.nurba.java.domain.Size;
import com.nurba.java.dto.request.CreateSizeRequest;
import com.nurba.java.dto.responce.SizeResponse;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.SizeMapper;
import com.nurba.java.repositories.SizeRepository;
import com.nurba.java.service.SizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SizeServiceImpl implements SizeService {

    private final SizeRepository repository;
    private final SizeMapper mapper;

    @Override
    public List<SizeResponse> getAll() {
        return repository.findAllByOrderBySortOrderAsc().stream().map(mapper::toResponse).toList();
    }

    @Override
    public SizeResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public SizeResponse create(CreateSizeRequest request) {
        Size entity = mapper.toEntity(request);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public SizeResponse update(Long id, CreateSizeRequest request) {
        Size entity = findOrThrow(id);
        entity.setLabel(request.getLabel());
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

    private Size findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Size not found: " + id));
    }
}
