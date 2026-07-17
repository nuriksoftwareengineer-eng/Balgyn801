package com.nurba.java.payment;

/** PayPal-specific alias of {@link PaymentProviderException} — same generic client handling. */
public class PayPalApiException extends PaymentProviderException {

    public PayPalApiException(String message) {
        super(message);
    }

    public PayPalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
