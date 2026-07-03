package com.nurba.java.api;

import com.nurba.java.dto.responce.ShopReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Reviews", description = "Публичные отзывы")
@RequestMapping("/api/v1/shop-reviews")
public interface ShopReviewPublicApi {

    @Operation(summary = "Получить опубликованные отзывы")
    @GetMapping
    List<ShopReviewResponse> getPublished(
            @RequestParam(defaultValue = "20") int limit);
}
