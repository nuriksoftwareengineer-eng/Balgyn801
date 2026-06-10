package com.nurba.java.controller;

import com.nurba.java.api.ReviewCatalogApi;
import com.nurba.java.dto.request.CreateReviewRequest;
import com.nurba.java.dto.responce.ReviewResponse;
import com.nurba.java.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController implements ReviewCatalogApi {

    private final ReviewService service;

    @Override
    public List<ReviewResponse> listForDesign(Long designId) {
        return service.listForDesign(designId);
    }

    @Override
    public ReviewResponse create(Long designId, CreateReviewRequest request, UserDetails userDetails) {
        return service.create(designId, request, userDetails.getUsername());
    }
}
