package com.nurba.java.service;

import com.nurba.java.dto.request.CreateReviewRequest;
import com.nurba.java.dto.responce.ReviewResponse;

import java.util.List;

public interface ReviewService {

    List<ReviewResponse> listForDesign(Long designId);

    ReviewResponse create(Long designId, CreateReviewRequest request, String userEmail);

    /** Admin operation — deletes any review regardless of owner. */
    void deleteAdmin(Long id);
}
