package com.nurba.java.service.Impl;

import com.nurba.java.config.StorageProperties;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.enabled", havingValue = "true", matchIfMissing = true)
public class MinioMediaStorageService implements MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioMediaStorageService.class);
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
        validateMagicBytes(file);

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
        } catch (SdkClientException e) {
            throw new BusinessRuleException(
                    "MinIO недоступен (" + props.endpoint() + "). Запустите MinIO или используйте docker compose up.");
        } catch (IOException e) {
            throw new BusinessRuleException("Не удалось прочитать файл: " + e.getMessage());
        }

        String base = props.publicBaseUrl().replaceAll("/+$", "");
        return base + "/" + props.bucket() + "/" + key;
    }

    private static void validateMagicBytes(MultipartFile file) {
        byte[] header = new byte[12];
        int bytesRead;
        try (InputStream in = file.getInputStream()) {
            bytesRead = in.read(header);
        } catch (IOException e) {
            throw new BusinessRuleException("Не удалось прочитать файл: " + e.getMessage());
        }
        if (bytesRead < 3) {
            throw new BusinessRuleException("Файл слишком короткий или повреждён");
        }
        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) return;
        // PNG: 89 50 4E 47
        if (bytesRead >= 4 && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) return;
        // GIF87a / GIF89a: 47 49 46 38
        if (bytesRead >= 4 && header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38) return;
        // WebP: 52 49 46 46 ?? ?? ?? ?? 57 45 42 50
        if (bytesRead >= 12 && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) return;
        throw new BusinessRuleException("Недопустимый формат файла. Разрешены: JPEG, PNG, GIF, WebP");
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
