package com.nurba.java.service.Impl;

import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.service.MediaStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "app.storage.enabled", havingValue = "false")
public class DisabledMediaStorageService implements MediaStorageService {

    @Override
    public String uploadPublicImage(MultipartFile file, String folder) {
        throw new BusinessRuleException(
                "Загрузка в хранилище отключена (app.storage.enabled=false)");
    }
}
