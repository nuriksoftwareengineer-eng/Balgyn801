package com.nurba.java.service.Impl;

import com.nurba.java.domain.Color;
import com.nurba.java.dto.request.CreateColorRequest;
import com.nurba.java.dto.responce.ColorResponse;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.ColorMapper;
import com.nurba.java.repositories.ColorRepository;
import com.nurba.java.service.ColorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ColorServiceImpl implements ColorService {

    private final ColorRepository repository;
    private final ColorMapper mapper;

    @Override
    public List<ColorResponse> getAll() {
        return repository.findAllByOrderBySortOrderAsc().stream().map(mapper::toResponse).toList();
    }

    @Override
    public ColorResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public ColorResponse create(CreateColorRequest request) {
        Color entity = mapper.toEntity(request);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public ColorResponse update(Long id, CreateColorRequest request) {
        Color entity = findOrThrow(id);
        entity.setName(request.getName());
        entity.setHexCode(request.getHexCode());
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

    private Color findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Color not found: " + id));
    }
}
