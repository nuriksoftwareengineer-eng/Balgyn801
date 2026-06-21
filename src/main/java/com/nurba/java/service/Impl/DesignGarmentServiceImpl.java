package com.nurba.java.service.Impl;

import com.nurba.java.component.DesignReadinessService;
import com.nurba.java.domain.Color;
import com.nurba.java.domain.Design;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.Size;
import com.nurba.java.dto.request.CreateDesignGarmentRequest;
import com.nurba.java.dto.request.UpdateDesignGarmentRequest;
import com.nurba.java.dto.responce.DesignGarmentResponse;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.DesignGarmentMapper;
import com.nurba.java.repositories.ColorRepository;
import com.nurba.java.repositories.DesignGarmentRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.repositories.SizeRepository;
import com.nurba.java.service.DesignGarmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DesignGarmentServiceImpl implements DesignGarmentService {

    private final DesignGarmentRepository repository;
    private final DesignRepository designRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final DesignGarmentMapper mapper;
    private final DesignReadinessService readinessService;

    @Override
    @Transactional(readOnly = true)
    public List<DesignGarmentResponse> getByDesign(Long designId) {
        return repository.findByDesign_Id(designId).stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DesignGarmentResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public DesignGarmentResponse create(CreateDesignGarmentRequest request) {
        Design design = designRepository.findById(request.getDesignId())
                .orElseThrow(() -> new NotFoundException("Design not found: " + request.getDesignId()));

        Set<Color> colors = resolveColors(request.getColorIds());
        Set<Size> sizes = resolveSizes(request.getSizeIds());

        DesignGarment entity = mapper.toEntity(request);
        entity.setDesign(design);
        entity.setColors(colors);
        entity.setSizes(sizes);

        DesignGarmentResponse response = mapper.toResponse(repository.save(entity));
        readinessService.recompute(request.getDesignId());
        return response;
    }

    @Override
    @Transactional
    public DesignGarmentResponse update(Long id, UpdateDesignGarmentRequest request) {
        DesignGarment entity = findOrThrow(id);
        Long designId = entity.getDesign().getId();

        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        if (request.getColorIds() != null) {
            entity.setColors(resolveColors(request.getColorIds()));
        }
        if (request.getSizeIds() != null) {
            entity.setSizes(resolveSizes(request.getSizeIds()));
        }

        DesignGarmentResponse response = mapper.toResponse(repository.save(entity));
        readinessService.recompute(designId);
        return response;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DesignGarment entity = findOrThrow(id);
        Long designId = entity.getDesign().getId();
        repository.deleteById(id);
        readinessService.recompute(designId);
    }

    private DesignGarment findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Design garment not found: " + id));
    }

    private Set<Color> resolveColors(List<Long> ids) {
        Set<Color> result = new LinkedHashSet<>();
        for (Long id : ids) {
            result.add(colorRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Color not found: " + id)));
        }
        return result;
    }

    private Set<Size> resolveSizes(List<Long> ids) {
        Set<Size> result = new LinkedHashSet<>();
        for (Long id : ids) {
            result.add(sizeRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Size not found: " + id)));
        }
        return result;
    }
}
