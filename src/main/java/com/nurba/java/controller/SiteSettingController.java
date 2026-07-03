package com.nurba.java.controller;

import com.nurba.java.service.SiteSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SiteSettingController {

    private final SiteSettingService service;

    /** Public: storefront fetches CEO photo and other public settings. */
    @GetMapping("/site-settings")
    public Map<String, String> getPublicSettings() {
        return service.getPublicSettings();
    }

    /** Admin: get any setting by key. */
    @GetMapping("/admin/site-settings/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> getOne(@PathVariable String key) {
        String value = service.get(key);
        return ResponseEntity.ok(Map.of("key", key, "value", value != null ? value : ""));
    }

    /** Admin: upsert a setting. Body: { "value": "..." } */
    @PutMapping("/admin/site-settings/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> set(@PathVariable String key, @RequestBody Map<String, String> body) {
        String value = service.set(key, body.get("value"));
        return Map.of("key", key, "value", value != null ? value : "");
    }
}
