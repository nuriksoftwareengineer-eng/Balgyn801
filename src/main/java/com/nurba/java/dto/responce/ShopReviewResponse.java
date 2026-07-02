package com.nurba.java.dto.responce;

import com.nurba.java.enums.ShopReviewStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ShopReviewResponse(
        Long id,
        String name,
        String avatarUrl,
        String city,
        Integer rating,
        String body,
        List<String> photoUrls,
        ShopReviewStatus status,
        LocalDateTime createdAt
) {}
