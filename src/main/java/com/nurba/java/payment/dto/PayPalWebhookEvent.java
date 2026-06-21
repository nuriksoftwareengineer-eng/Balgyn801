package com.nurba.java.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PayPalWebhookEvent(
        String id,
        @JsonProperty("event_type") String eventType,
        JsonNode resource
) {}
