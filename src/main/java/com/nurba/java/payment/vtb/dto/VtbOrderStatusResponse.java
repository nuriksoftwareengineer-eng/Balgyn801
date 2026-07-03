package com.nurba.java.payment.vtb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VtbOrderStatusResponse(
        @JsonProperty("orderStatus") Integer orderStatus,
        @JsonProperty("errorCode") Integer errorCode,
        @JsonProperty("errorMessage") String errorMessage,
        @JsonProperty("amount") Long amount,
        @JsonProperty("currency") Integer currency,
        @JsonProperty("orderNumber") String orderNumber
) {
    public boolean isSuccess() {
        return errorCode == null || errorCode == 0;
    }
}
