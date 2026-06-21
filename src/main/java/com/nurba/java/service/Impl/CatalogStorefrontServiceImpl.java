package com.nurba.java.service.Impl;

import com.nurba.java.domain.CatalogGroup;
import com.nurba.java.domain.Collection;
import com.nurba.java.domain.Design;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.dto.responce.CatalogGroupDetailResponse;
import com.nurba.java.dto.responce.CatalogGroupResponse;
import com.nurba.java.dto.responce.CollectionDetailResponse;
import com.nurba.java.dto.responce.CollectionResponse;
import com.nurba.java.dto.responce.DesignDetailResponse;
import com.nurba.java.dto.responce.DesignGarmentResponse;
import com.nurba.java.dto.responce.DesignResponse;
import com.nurba.java.enums.DesignStatus;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.CatalogGroupMapper;
import com.nurba.java.mapper.CollectionMapper;
import com.nurba.java.mapper.DesignGarmentMapper;
import com.nurba.java.mapper.DesignMapper;
import com.nurba.java.domain.Inventory;
import com.nurba.java.repositories.CatalogGroupRepository;
import com.nurba.java.repositories.CollectionRepository;
import com.nurba.java.repositories.DesignGarmentRepository;
import com.nurba.java.repositories.DesignRepository;
import com.nurba.java.repositories.InventoryRepository;
import com.nurba.java.service.CatalogStorefrontService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogStorefrontServiceImpl implements CatalogStorefrontService {

    private final CatalogGroupRepository groupRepository;
    private final CollectionRepository collectionRepository;
    private final DesignRepository designRepository;
    private final DesignGarmentRepository garmentRepository;
    private final InventoryRepository inventoryRepository;

    private final CatalogGroupMapper groupMapper;
    private final CollectionMapper collectionMapper;
    private final DesignMapper designMapper;
    private final DesignGarmentMapper garmentMapper;

    @Override
    public List<CatalogGroupResponse> getGroups() {
        return groupRepository.findAllByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(groupMapper::toResponse)
                .toList();
    }

    @Override
    public CatalogGroupDetailResponse getGroupBySlug(String slug) {
        CatalogGroup group = groupRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new NotFoundException("Catalog group not found: " + slug));

        List<CollectionResponse> collections =
                collectionRepository.findByCatalogGroup_IdAndActiveTrueOrderBySortOrderAsc(group.getId())
                        .stream()
                        .map(collectionMapper::toResponse)
                        .toList();

        CatalogGroupDetailResponse response = new CatalogGroupDetailResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setSlug(group.getSlug());
        response.setSortOrder(group.getSortOrder());
        response.setCollections(collections);
        return response;
    }

    @Override
    public CollectionDetailResponse getCollectionBySlug(String slug) {
        Collection collection = collectionRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new NotFoundException("Collection not found: " + slug));

        // EntityGraph on findByCollection_IdAndStatusOrderByCreatedAtDesc eagerly loads
        // collection.catalogGroup — eliminates N+1 for groupName/groupSlug mapping (F-07)
        List<DesignResponse> designs =
                designRepository.findByCollection_IdAndStatusOrderByCreatedAtDesc(
                        collection.getId(), DesignStatus.PUBLISHED)
                        .stream()
                        .map(designMapper::toResponse)
                        .toList();

        CollectionDetailResponse response = new CollectionDetailResponse();
        response.setId(collection.getId());
        response.setGroupId(collection.getCatalogGroup().getId());
        response.setGroupName(collection.getCatalogGroup().getName());
        response.setName(collection.getName());
        response.setSlug(collection.getSlug());
        response.setDescription(collection.getDescription());
        response.setCoverImageUrl(collection.getCoverImageUrl());
        response.setBannerImageUrl(collection.getBannerImageUrl());
        response.setDesigns(designs);
        return response;
    }

    @Override
    public List<DesignResponse> getDesigns(Long collectionId) {
        List<Design> designs = (collectionId != null)
                ? designRepository.findByCollection_IdAndStatusOrderByCreatedAtDesc(collectionId, DesignStatus.PUBLISHED)
                : designRepository.findAllByStatusOrderByCreatedAtDesc(DesignStatus.PUBLISHED);
        return designs.stream().map(designMapper::toResponse).toList();
    }

    @Override
    public DesignDetailResponse getDesignBySlug(String slug) {
        Design design = designRepository.findBySlugAndStatus(slug, DesignStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Design not found: " + slug));

        List<DesignGarment> garments =
                garmentRepository.findByDesign_IdAndActiveTrue(design.getId());

        // Bulk-fetch all inventory rows for all garments in one query (avoids N+1).
        List<Long> garmentIds = garments.stream().map(DesignGarment::getId).toList();
        Map<Long, List<Inventory>> inventoryByGarmentId = inventoryRepository
                .findByDesignGarment_IdIn(garmentIds)
                .stream()
                .collect(Collectors.groupingBy(inv -> inv.getDesignGarment().getId()));

        List<DesignGarmentResponse> garmentResponses = garments.stream()
                .map(garment -> {
                    DesignGarmentResponse resp = garmentMapper.toResponse(garment);
                    List<Inventory> rows = inventoryByGarmentId.getOrDefault(garment.getId(), List.of());
                    Map<Long, Map<Long, Integer>> stockMap = new HashMap<>();
                    for (Inventory inv : rows) {
                        stockMap
                            .computeIfAbsent(inv.getColor().getId(), k -> new HashMap<>())
                            .put(inv.getSize().getId(), inv.getQuantity());
                    }
                    resp.setStockMap(stockMap);
                    return resp;
                })
                .toList();

        DesignDetailResponse response = new DesignDetailResponse();
        response.setId(design.getId());
        response.setName(design.getName());
        response.setSlug(design.getSlug());
        response.setDescription(design.getDescription());
        response.setMainImageUrl(design.getMainImageUrl());
        response.setGallery(design.getGallery());
        response.setCollectionId(design.getCollection().getId());
        response.setCollectionName(design.getCollection().getName());
        response.setCollectionSlug(design.getCollection().getSlug());
        response.setGroupName(design.getCollection().getCatalogGroup().getName());
        response.setGroupSlug(design.getCollection().getCatalogGroup().getSlug());
        response.setGarments(garmentResponses);
        return response;
    }
}
