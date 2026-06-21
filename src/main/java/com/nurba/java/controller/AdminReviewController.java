package com.nurba.java.controller;

import com.nurba.java.api.AdminReviewApi;
import com.nurba.java.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController implements AdminReviewApi {

    private final ReviewService service;

    @Override
    public void delete(Long id) {
        service.deleteAdmin(id);
    }
}
