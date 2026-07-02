package com.nurba.java.controller;

import com.nurba.java.api.ShopReviewPublicApi;
import com.nurba.java.dto.responce.ShopReviewResponse;
import com.nurba.java.service.ShopReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShopReviewController implements ShopReviewPublicApi {

    private final ShopReviewService service;

    @Override
    public List<ShopReviewResponse> getPublished(int limit) {
        return service.getPublished(Math.min(limit, 100));
    }
}
