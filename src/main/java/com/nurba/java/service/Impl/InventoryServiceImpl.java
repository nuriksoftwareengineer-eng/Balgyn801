package com.nurba.java.service.Impl;

import com.nurba.java.domain.Color;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.Inventory;
import com.nurba.java.domain.Size;
import com.nurba.java.dto.request.SetInventoryRequest;
import com.nurba.java.dto.responce.InventoryResponse;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.InventoryMapper;
import com.nurba.java.repositories.ColorRepository;
import com.nurba.java.repositories.DesignGarmentRepository;
import com.nurba.java.repositories.InventoryRepository;
import com.nurba.java.repositories.SizeRepository;
import com.nurba.java.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository repository;
    private final DesignGarmentRepository garmentRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final InventoryMapper mapper;

    @Override
    public List<InventoryResponse> getByGarment(Long designGarmentId) {
        return repository.findByDesignGarment_Id(designGarmentId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Inventory record not found: " + id);
        }
        repository.deleteById(id);
    }

    @Override
    public InventoryResponse set(SetInventoryRequest request) {
        DesignGarment garment = garmentRepository.findById(request.getDesignGarmentId())
                .orElseThrow(() -> new NotFoundException("Design garment not found: " + request.getDesignGarmentId()));
        Color color = colorRepository.findById(request.getColorId())
                .orElseThrow(() -> new NotFoundException("Color not found: " + request.getColorId()));
        Size size = sizeRepository.findById(request.getSizeId())
                .orElseThrow(() -> new NotFoundException("Size not found: " + request.getSizeId()));

        Inventory entity = repository
                .findByDesignGarment_IdAndColor_IdAndSize_Id(
                        request.getDesignGarmentId(), request.getColorId(), request.getSizeId())
                .orElseGet(Inventory::new);

        entity.setDesignGarment(garment);
        entity.setColor(color);
        entity.setSize(size);
        entity.setQuantity(request.getQuantity());

        return mapper.toResponse(repository.save(entity));
    }
}
