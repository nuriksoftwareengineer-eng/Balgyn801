package com.nurba.java.tracking;

/**
 * Strategy interface for parcel tracking providers.
 * Implement to add a new carrier API (17TRACK, AfterShip, direct Kazpost, etc.).
 */
public interface ParcelTrackingProvider {

    /** Unique identifier used in parcel_trackings.provider column. */
    String providerName();

    /** True when the integration is properly configured (API key present). */
    boolean isAvailable();

    /**
     * Fetches latest tracking data from the external API.
     *
     * @param trackingNumber the shipment tracking number
     * @param carrier        carrier code understood by this provider (e.g. "KAZPOST")
     * @return tracking result, or null on provider error (caller keeps last-known-good)
     */
    TrackingResult fetch(String trackingNumber, String carrier);
}
