package com.nurba.java.payment.vtb.dto;

public record VtbRegisterRequest(
        String orderNumber,
        long amount,
        int currency,
        String returnUrl,
        String description,
        String callbackUrl
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String orderNumber;
        private long amount;
        private int currency;
        private String returnUrl;
        private String description;
        private String callbackUrl;

        public Builder orderNumber(String v) { orderNumber = v; return this; }
        public Builder amount(long v)        { amount = v;       return this; }
        public Builder currency(int v)       { currency = v;     return this; }
        public Builder returnUrl(String v)   { returnUrl = v;    return this; }
        public Builder description(String v) { description = v;  return this; }
        public Builder callbackUrl(String v) { callbackUrl = v;  return this; }

        public VtbRegisterRequest build() {
            return new VtbRegisterRequest(orderNumber, amount, currency, returnUrl, description, callbackUrl);
        }
    }
}
