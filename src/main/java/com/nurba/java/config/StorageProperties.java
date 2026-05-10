package com.nurba.java.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        boolean enabled,
        String endpoint,
        String publicBaseUrl,
        String accessKey,
        String secretKey,
        String bucket,
        String region
) {
}
