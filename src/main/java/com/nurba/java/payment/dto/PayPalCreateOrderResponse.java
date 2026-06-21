package com.nurba.java.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PayPalCreateOrderResponse(
        String id,
        String status,
        List<Link> links
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Link(String href, String rel, String method) {}

    /** Returns the payer-action (approval) URL from the links array, or null. */
    public String approvalUrl() {
        if (links == null) return null;
        return links.stream()
                .filter(l -> "payer-action".equals(l.rel()) || "approve".equals(l.rel()))
                .map(Link::href)
                .findFirst()
                .orElse(null);
    }
}
