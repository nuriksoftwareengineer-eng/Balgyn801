package com.nurba.java.controller;

import com.nurba.java.api.AdminShopReviewApi;
import com.nurba.java.dto.request.ShopReviewRequest;
import com.nurba.java.dto.responce.PageResponse;
import com.nurba.java.dto.responce.ShopReviewResponse;
import com.nurba.java.service.ShopReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminShopReviewController implements AdminShopReviewApi {

    private final ShopReviewService service;

    @Override
    public PageResponse<ShopReviewResponse> list(String q, int page, int size) {
        return service.listAdmin(q, page, size);
    }

    @Override
    public ShopReviewResponse create(ShopReviewRequest req) {
        return service.create(req);
    }

    @Override
    public ShopReviewResponse update(Long id, ShopReviewRequest req) {
        return service.update(id, req);
    }

    @Override
    public ShopReviewResponse publish(Long id) {
        return service.publish(id);
    }

    @Override
    public ShopReviewResponse hide(Long id) {
        return service.hide(id);
    }

    @Override
    public void delete(Long id) {
        service.delete(id);
    }
}
