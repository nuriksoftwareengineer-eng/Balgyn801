package com.nurba.java.payment.vtb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VtbRegisterResponse(
        @JsonProperty("orderId") String orderId,
        @JsonProperty("formUrl") String formUrl,
        @JsonProperty("errorCode") Integer errorCode,
        @JsonProperty("errorMessage") String errorMessage
) {
    public boolean isSuccess() {
        return (errorCode == null || errorCode == 0)
                && orderId != null && !orderId.isBlank();
    }
}
