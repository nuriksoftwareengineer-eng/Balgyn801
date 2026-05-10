package com.nurba.java.controller;

import com.nurba.java.api.CustomDesignApi;
import com.nurba.java.dto.request.CreateCustomDesignRequest;
import com.nurba.java.dto.responce.CustomDesignResponse;
import com.nurba.java.service.CustomDesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CustomDesignController implements CustomDesignApi {

    private final CustomDesignService customDesignService;

    @Override
    public List<CustomDesignResponse> getAll() {
        return customDesignService.getAll();
    }

    @Override
    public CustomDesignResponse getById(Long id) {
        return customDesignService.getById(id);
    }

    @Override
    public CustomDesignResponse create(CreateCustomDesignRequest request) {
        return customDesignService.create(request);
    }
}
