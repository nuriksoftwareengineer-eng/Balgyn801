package com.nurba.java.api;

import com.nurba.java.dto.request.ShopReviewRequest;
import com.nurba.java.dto.responce.PageResponse;
import com.nurba.java.dto.responce.ShopReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin / Shop Reviews", description = "Управление отзывами")
@RequestMapping("/api/v1/admin/shop-reviews")
public interface AdminShopReviewApi {

    @Operation(summary = "Поиск отзывов с пагинацией")
    @GetMapping
    PageResponse<ShopReviewResponse> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size);

    @Operation(summary = "Создать отзыв")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ShopReviewResponse create(@RequestBody @Valid ShopReviewRequest req);

    @Operation(summary = "Обновить отзыв")
    @PutMapping("/{id}")
    ShopReviewResponse update(@PathVariable Long id, @RequestBody @Valid ShopReviewRequest req);

    @Operation(summary = "Опубликовать отзыв")
    @PatchMapping("/{id}/publish")
    ShopReviewResponse publish(@PathVariable Long id);

    @Operation(summary = "Скрыть отзыв")
    @PatchMapping("/{id}/hide")
    ShopReviewResponse hide(@PathVariable Long id);

    @Operation(summary = "Удалить отзыв")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id);
}
