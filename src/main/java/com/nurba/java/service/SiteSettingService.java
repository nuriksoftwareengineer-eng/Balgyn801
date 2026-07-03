package com.nurba.java.service;

import java.util.Map;

public interface SiteSettingService {

    /** Returns a single setting value, or null if not set. */
    String get(String key);

    /** Upserts a single setting. Returns the new value. */
    String set(String key, String value);

    /** Returns all public settings (used by the storefront). */
    Map<String, String> getPublicSettings();
}
