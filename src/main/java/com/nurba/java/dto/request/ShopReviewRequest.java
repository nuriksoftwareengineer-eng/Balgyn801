package com.nurba.java.dto.request;

import com.nurba.java.enums.ShopReviewStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ShopReviewRequest(
        @NotBlank String name,
        String avatarUrl,
        String city,
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank String body,
        List<String> photoUrls,
        ShopReviewStatus status
) {}
