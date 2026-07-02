package com.nurba.java.controller;

import com.nurba.java.api.CatalogStorefrontApi;
import com.nurba.java.dto.responce.CatalogGroupDetailResponse;
import com.nurba.java.dto.responce.CatalogGroupResponse;
import com.nurba.java.dto.responce.CollectionDetailResponse;
import com.nurba.java.dto.responce.DesignDetailResponse;
import com.nurba.java.dto.responce.DesignResponse;
import com.nurba.java.service.CatalogStorefrontService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CatalogStorefrontController implements CatalogStorefrontApi {

    private final CatalogStorefrontService service;

    @Override
    public List<CatalogGroupResponse> getGroups() {
        return service.getGroups();
    }

    @Override
    public CatalogGroupDetailResponse getGroupBySlug(String slug) {
        return service.getGroupBySlug(slug);
    }

    @Override
    public CollectionDetailResponse getCollectionBySlug(String slug) {
        return service.getCollectionBySlug(slug);
    }

    @Override
    public List<DesignResponse> getDesigns(Long collectionId) {
        return service.getDesigns(collectionId);
    }

    @Override
    public DesignDetailResponse getDesignBySlug(String slug) {
        return service.getDesignBySlug(slug);
    }

    @Override
    public List<DesignResponse> getPopular(int limit) {
        return service.getPopular(limit);
    }

    @Override
    public List<DesignResponse> getNewArrivals(int limit) {
        return service.getNewArrivals(limit);
    }

    @Override
    public List<DesignResponse> getRecommendations(Long designId, int limit) {
        return service.getRecommendations(designId, limit);
    }
}
