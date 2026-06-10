package com.nurba.java.service;

import com.nurba.java.dto.request.CreateCollectionRequest;
import com.nurba.java.dto.responce.CollectionResponse;

import java.util.List;

public interface CollectionService {
    List<CollectionResponse> getAll(Long groupId);
    CollectionResponse getById(Long id);
    CollectionResponse create(CreateCollectionRequest request);
    CollectionResponse update(Long id, CreateCollectionRequest request);
    void delete(Long id);
}
