package com.nurba.java.controller;

import com.nurba.java.api.AdminReviewApi;
import com.nurba.java.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminReviewController implements AdminReviewApi {

    private final ReviewService service;

    @Override
    public void delete(Long id) {
        service.deleteAdmin(id);
    }
}
