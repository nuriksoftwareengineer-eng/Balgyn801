package com.nurba.java.controller;

import com.nurba.java.api.CollectionApi;
import com.nurba.java.dto.request.CreateCollectionRequest;
import com.nurba.java.dto.responce.CollectionResponse;
import com.nurba.java.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CollectionController implements CollectionApi {

    private final CollectionService service;

    @Override
    public List<CollectionResponse> getAll(Long groupId) {
        return service.getAll(groupId);
    }

    @Override
    public CollectionResponse getById(Long id) {
        return service.getById(id);
    }

    @Override
    public CollectionResponse create(CreateCollectionRequest request) {
        return service.create(request);
    }

    @Override
    public CollectionResponse update(Long id, CreateCollectionRequest request) {
        return service.update(id, request);
    }

    @Override
    public void delete(Long id) {
        service.delete(id);
    }
}
