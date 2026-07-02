package com.nurba.java.tracking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.ParcelTracking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 17TRACK provider (https://www.17track.net/en/apidoc).
 * Supports Kazpost (carrier code: "PostKZ"), CDEK, and 2000+ other carriers.
 * Configured via SEVENTEEN_TRACK_API_KEY env var; stubs on blank key.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeventeenTrackProvider implements ParcelTrackingProvider {

    private static final String API_URL = "https://api.17track.net/track/v2.2/gettrackinfo";

    @Value("${app.tracking.seventeen-track.api-key:}")
    private String apiKey;

    private final ObjectMapper objectMapper;

    @Override
    public String providerName() {
        return "17TRACK";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public TrackingResult fetch(String trackingNumber, String carrier) {
        if (!isAvailable()) {
            log.debug("17TRACK not configured — returning stub for {}", trackingNumber);
            return stubResult(trackingNumber);
        }
        try {
            String body = objectMapper.writeValueAsString(
                    List.of(new TrackRequest(trackingNumber, carrierToSlug(carrier)))
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("17token", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            return parse(response.body());
        } catch (Exception e) {
            log.warn("17TRACK fetch failed for {}: {}", trackingNumber, e.getMessage());
            return null;
        }
    }

    private TrackingResult parse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode data = root.path("data").path("accepted").get(0);
        if (data == null || data.isMissingNode()) {
            return null;
        }
        JsonNode track = data.path("track");
        String lastStatus = track.path("e").asText("PENDING");
        String statusDetail = track.path("z1").path("z").asText("");

        List<ParcelTracking.TrackingEvent> events = new ArrayList<>();
        for (JsonNode event : track.path("z")) {
            events.add(new ParcelTracking.TrackingEvent(
                    event.path("a").asText(),
                    event.path("c").asText(),
                    event.path("z").asText(),
                    event.path("e").asText()
            ));
        }

        return new TrackingResult(mapStatus(lastStatus), statusDetail, events);
    }

    private String mapStatus(String code) {
        return switch (code) {
            case "NotFound"    -> "NOT_FOUND";
            case "InTransit"   -> "IN_TRANSIT";
            case "Delivered"   -> "DELIVERED";
            case "Undelivered" -> "UNDELIVERED";
            case "Returned"    -> "RETURNED";
            case "Expired"     -> "EXPIRED";
            default            -> "PENDING";
        };
    }

    /** Maps internal carrier names to 17TRACK carrier slugs. */
    private String carrierToSlug(String carrier) {
        return switch (carrier.toUpperCase()) {
            case "KAZPOST" -> "PostKZ";
            case "CDEK"    -> "CDEK";
            default        -> carrier;
        };
    }

    private TrackingResult stubResult(String trackingNumber) {
        return new TrackingResult(
                "PENDING",
                "Tracking will be available once the API key is configured.",
                List.of(new ParcelTracking.TrackingEvent(
                        null, null, "Tracking not yet configured.", "PENDING"
                ))
        );
    }

    private record TrackRequest(String number, String carrier) {}
}
