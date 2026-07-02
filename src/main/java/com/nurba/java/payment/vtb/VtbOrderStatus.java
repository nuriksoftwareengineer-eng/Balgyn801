package com.nurba.java.payment.vtb;

import com.nurba.java.enums.PaymentStatus;

public enum VtbOrderStatus {
    REGISTERED(0),
    PRE_AUTHORIZED(1),
    DEPOSITED(2),
    DECLINED(3),
    REVERSED(4),
    REFUNDED(5),
    UNKNOWN(-1);

    private final int code;

    VtbOrderStatus(int code) { this.code = code; }

    public int getCode() { return code; }

    public static VtbOrderStatus fromCode(int code) {
        for (VtbOrderStatus s : values()) {
            if (s.code == code) return s;
        }
        return UNKNOWN;
    }

    public PaymentStatus toPaymentStatus() {
        return switch (this) {
            case DEPOSITED                   -> PaymentStatus.SUCCEEDED;
            case DECLINED, REVERSED         -> PaymentStatus.FAILED;
            case REFUNDED                   -> PaymentStatus.REFUNDED;
            default                         -> PaymentStatus.PENDING;
        };
    }
}
