package com.nurba.java.controller;

import com.nurba.java.dto.responce.MediaUploadResponse;
import com.nurba.java.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MediaUploadController {

    private final MediaStorageService mediaStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaUploadResponse upload(@RequestParam("file") MultipartFile file) {
        String publicUrl = mediaStorageService.uploadPublicImage(file, "products");
        return new MediaUploadResponse(publicUrl);
    }
}
