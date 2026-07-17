package com.nurba.java.payment;

/**
 * A payment provider's own API rejected, failed, or was unreachable for a request (bad
 * credentials, signature mismatch, provider downtime, transport error, etc). The message is
 * for logs only — RestExceptionHandler logs it in full and returns a generic, translated
 * message to the client; provider/API internals must never reach the browser.
 */
public class PaymentProviderException extends RuntimeException {

    public PaymentProviderException(String message) {
        super(message);
    }

    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
