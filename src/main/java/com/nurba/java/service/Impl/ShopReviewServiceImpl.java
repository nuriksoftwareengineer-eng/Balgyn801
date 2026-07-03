package com.nurba.java.service.Impl;

import com.nurba.java.domain.ShopReview;
import com.nurba.java.dto.request.ShopReviewRequest;
import com.nurba.java.dto.responce.PageResponse;
import com.nurba.java.dto.responce.ShopReviewResponse;
import com.nurba.java.enums.ShopReviewStatus;
import com.nurba.java.repositories.ShopReviewRepository;
import com.nurba.java.service.ShopReviewService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShopReviewServiceImpl implements ShopReviewService {

    private final ShopReviewRepository repo;

    @Override
    @Transactional(readOnly = true)
    public List<ShopReviewResponse> getPublished(int limit) {
        var pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return repo.findByStatus(ShopReviewStatus.PUBLISHED, pageable)
                   .stream()
                   .map(this::toResponse)
                   .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShopReviewResponse> listAdmin(String q, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var result = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.search(q, pageable);
        return PageResponse.of(result.map(this::toResponse));
    }

    @Override
    public ShopReviewResponse create(ShopReviewRequest req) {
        var entity = new ShopReview();
        entity.setCreatedAt(LocalDateTime.now());
        applyRequest(entity, req);
        return toResponse(repo.save(entity));
    }

    @Override
    public ShopReviewResponse update(Long id, ShopReviewRequest req) {
        var entity = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ShopReview not found: " + id));
        applyRequest(entity, req);
        return toResponse(repo.save(entity));
    }

    @Override
    public ShopReviewResponse publish(Long id) {
        var entity = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ShopReview not found: " + id));
        entity.setStatus(ShopReviewStatus.PUBLISHED);
        return toResponse(repo.save(entity));
    }

    @Override
    public ShopReviewResponse hide(Long id) {
        var entity = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ShopReview not found: " + id));
        entity.setStatus(ShopReviewStatus.HIDDEN);
        return toResponse(repo.save(entity));
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    private void applyRequest(ShopReview entity, ShopReviewRequest req) {
        entity.setName(req.name());
        entity.setAvatarUrl(req.avatarUrl());
        entity.setCity(req.city());
        entity.setRating(req.rating());
        entity.setBody(req.body());
        entity.setPhotoUrls(req.photoUrls() != null ? req.photoUrls() : new ArrayList<>());
        entity.setStatus(req.status() != null ? req.status() : ShopReviewStatus.PUBLISHED);
    }

    private ShopReviewResponse toResponse(ShopReview r) {
        return new ShopReviewResponse(
                r.getId(), r.getName(), r.getAvatarUrl(), r.getCity(),
                r.getRating(), r.getBody(),
                r.getPhotoUrls() != null ? r.getPhotoUrls() : List.of(),
                r.getStatus(), r.getCreatedAt()
        );
    }
}
