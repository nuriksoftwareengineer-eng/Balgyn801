package com.nurba.java.service;

import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {

    /**
     * Загружает файл и возвращает публичный URL для вставки в {@code Product.imageUrl}.
     */
    String uploadPublicImage(MultipartFile file, String folder);
}
