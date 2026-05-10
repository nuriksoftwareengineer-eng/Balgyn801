package com.nurba.java.service.Impl;

import com.nurba.java.config.StorageProperties;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.enabled", havingValue = "true", matchIfMissing = true)
public class MinioMediaStorageService implements MediaStorageService {

    private static final long MAX_BYTES = 8 * 1024 * 1024;

    private final S3Client s3Client;
    private final StorageProperties props;

    @Override
    public String uploadPublicImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Файл пустой");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessRuleException("Разрешены только изображения (image/*)");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessRuleException("Файл слишком большой (максимум 8 МБ)");
        }

        String safeFolder = folder == null || folder.isBlank() ? "uploads" : folder.replaceAll("^/+|/+$", "");
        String ext = extensionFrom(file.getOriginalFilename(), contentType);
        String key = safeFolder + "/" + UUID.randomUUID() + ext;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(props.bucket())
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new BusinessRuleException("Не удалось прочитать файл: " + e.getMessage());
        }

        String base = props.publicBaseUrl().replaceAll("/+$", "");
        return base + "/" + props.bucket() + "/" + key;
    }

    private static String extensionFrom(String originalName, String contentType) {
        if (originalName != null && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (ext.length() <= 8 && ext.matches("\\.[a-z0-9]+")) {
                return ext;
            }
        }
        if (contentType.contains("png")) {
            return ".png";
        }
        if (contentType.contains("webp")) {
            return ".webp";
        }
        if (contentType.contains("gif")) {
            return ".gif";
        }
        return ".jpg";
    }
}
