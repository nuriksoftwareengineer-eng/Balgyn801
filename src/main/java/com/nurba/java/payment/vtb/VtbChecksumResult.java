package com.nurba.java.payment.vtb;

public enum VtbChecksumResult {
    VALID,
    INVALID,
    SKIPPED,   // callbackSecret not configured
    ABSENT     // checksum param missing from callback
}
