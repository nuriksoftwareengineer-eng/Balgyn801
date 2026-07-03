package com.nurba.java.service;

import com.nurba.java.dto.request.ShopReviewRequest;
import com.nurba.java.dto.responce.PageResponse;
import com.nurba.java.dto.responce.ShopReviewResponse;

import java.util.List;

public interface ShopReviewService {
    List<ShopReviewResponse> getPublished(int limit);
    PageResponse<ShopReviewResponse> listAdmin(String q, int page, int size);
    ShopReviewResponse create(ShopReviewRequest req);
    ShopReviewResponse update(Long id, ShopReviewRequest req);
    ShopReviewResponse publish(Long id);
    ShopReviewResponse hide(Long id);
    void delete(Long id);
}
