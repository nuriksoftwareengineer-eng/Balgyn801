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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        response.setNameKk(group.getNameKk());
        response.setNameEn(group.getNameEn());
        response.setSlug(group.getSlug());
        response.setSortOrder(group.getSortOrder());
        response.setCoverImageUrl(group.getCoverImageUrl());
        response.setBannerImageUrl(group.getBannerImageUrl());
        response.setCollections(collections);
        return response;
    }

    @Override
    public CollectionDetailResponse getCollectionBySlug(String slug) {
        Collection collection = collectionRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new NotFoundException("Collection not found: " + slug));

        List<DesignResponse> designs =
                designRepository.findByCollection_IdAndStatusOrderByCreatedAtDesc(
                        collection.getId(), DesignStatus.PUBLISHED)
                        .stream()
                        .map(this::toResponseWithPrice)
                        .toList();

        CollectionDetailResponse response = new CollectionDetailResponse();
        response.setId(collection.getId());
        response.setGroupId(collection.getCatalogGroup().getId());
        response.setGroupName(collection.getCatalogGroup().getName());
        response.setGroupNameKk(collection.getCatalogGroup().getNameKk());
        response.setGroupNameEn(collection.getCatalogGroup().getNameEn());
        response.setName(collection.getName());
        response.setNameKk(collection.getNameKk());
        response.setNameEn(collection.getNameEn());
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
        return designs.stream().map(this::toResponseWithPrice).toList();
    }

    @Override
    @Transactional
    public DesignDetailResponse getDesignBySlug(String slug) {
        Design design = designRepository.findBySlugAndStatus(slug, DesignStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Design not found: " + slug));

        designRepository.incrementViewCount(design.getId());

        List<DesignGarment> garments =
                garmentRepository.findByDesign_IdAndActiveTrue(design.getId());

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
        response.setNameKk(design.getNameKk());
        response.setNameEn(design.getNameEn());
        response.setSlug(design.getSlug());
        response.setDescription(design.getDescription());
        response.setMainImageUrl(design.getMainImageUrl());
        response.setGallery(design.getGallery());
        response.setCollectionId(design.getCollection().getId());
        response.setCollectionName(design.getCollection().getName());
        response.setCollectionNameKk(design.getCollection().getNameKk());
        response.setCollectionNameEn(design.getCollection().getNameEn());
        response.setCollectionSlug(design.getCollection().getSlug());
        response.setGroupName(design.getCollection().getCatalogGroup().getName());
        response.setGroupNameKk(design.getCollection().getCatalogGroup().getNameKk());
        response.setGroupNameEn(design.getCollection().getCatalogGroup().getNameEn());
        response.setGroupSlug(design.getCollection().getCatalogGroup().getSlug());
        response.setGarments(garmentResponses);
        return response;
    }

    @Override
    public List<DesignResponse> getPopular(int limit) {
        return designRepository.findTopByViewCount(PageRequest.of(0, limit))
                .stream()
                .map(this::toResponseWithPrice)
                .toList();
    }

    @Override
    public List<DesignResponse> getNewArrivals(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        return designRepository.findNewArrivals(since, PageRequest.of(0, limit))
                .stream()
                .map(this::toResponseWithPrice)
                .toList();
    }

    @Override
    public List<DesignResponse> getRecommendations(Long designId, int limit) {
        Design design = designRepository.findById(designId).orElse(null);
        if (design == null) return List.of();
        List<DesignResponse> recs = designRepository
                .findRecommendations(design.getCollection().getId(), designId, PageRequest.of(0, limit))
                .stream()
                .map(this::toResponseWithPrice)
                .toList();
        if (recs.size() < limit) {
            // fallback: fill with popular
            List<DesignResponse> popular = getPopular(limit * 2);
            for (DesignResponse p : popular) {
                if (recs.size() >= limit) break;
                if (p.getId().equals(designId)) continue;
                if (recs.stream().noneMatch(r -> r.getId().equals(p.getId()))) {
                    recs = new java.util.ArrayList<>(recs);
                    recs.add(p);
                }
            }
        }
        return recs;
    }

    /** Карточкам каталога нужна цена «от»: минимальная KZT-цена по активным вариантам.
     *  Конвертация в выбранную валюту выполняется на фронте (CurrencyContext). */
    private DesignResponse toResponseWithPrice(Design design) {
        DesignResponse response = designMapper.toResponse(design);
        response.setMinPriceKzt(design.getGarments().stream()
                .filter(g -> Boolean.TRUE.equals(g.getActive()))
                .flatMap(g -> g.getPrices().stream())
                .filter(price -> price.getCurrency() == com.nurba.java.enums.Currency.KZT)
                .map(com.nurba.java.domain.DesignGarmentPrice::getAmount)
                .filter(java.util.Objects::nonNull)
                .min(java.util.Comparator.naturalOrder())
                .orElse(null));
        return response;
    }
}
