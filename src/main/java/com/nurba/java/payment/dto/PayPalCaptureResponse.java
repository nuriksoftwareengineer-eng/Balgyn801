package com.nurba.java.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PayPalCaptureResponse(
        String id,
        String status,
        @JsonProperty("purchase_units") List<PurchaseUnit> purchaseUnits
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PurchaseUnit(Payments payments) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payments(List<Capture> captures) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Capture(String id, String status, Amount amount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(String value, @JsonProperty("currency_code") String currencyCode) {
        public BigDecimal valueBd() {
            if (value == null) return null;
            try { return new BigDecimal(value); } catch (NumberFormatException e) { return null; }
        }
    }

    /** Returns the ID of the first capture, or null. */
    public String captureId() {
        Capture c = firstCapture();
        return c != null ? c.id() : null;
    }

    /** Returns the captured amount of the first capture, or null. */
    public BigDecimal capturedAmount() {
        Capture c = firstCapture();
        if (c == null || c.amount() == null) return null;
        return c.amount().valueBd();
    }

    private Capture firstCapture() {
        if (purchaseUnits == null || purchaseUnits.isEmpty()) return null;
        PurchaseUnit unit = purchaseUnits.get(0);
        if (unit.payments() == null) return null;
        List<Capture> captures = unit.payments().captures();
        if (captures == null || captures.isEmpty()) return null;
        return captures.get(0);
    }
}
