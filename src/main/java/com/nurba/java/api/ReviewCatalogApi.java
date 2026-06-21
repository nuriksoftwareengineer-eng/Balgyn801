package com.nurba.java.api;

import com.nurba.java.dto.request.CreateReviewRequest;
import com.nurba.java.dto.responce.ReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Reviews", description = "Design reviews — public read, authenticated write")
@RequestMapping("/api/v1/catalog/designs/{designId}/reviews")
public interface ReviewCatalogApi {

    @Operation(summary = "List reviews for a design (public)")
    @GetMapping
    List<ReviewResponse> listForDesign(@PathVariable Long designId);

    @Operation(summary = "Submit a review (authenticated, must have purchased design)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ReviewResponse create(@PathVariable Long designId,
                          @Valid @RequestBody CreateReviewRequest request,
                          @AuthenticationPrincipal UserDetails userDetails);
}
