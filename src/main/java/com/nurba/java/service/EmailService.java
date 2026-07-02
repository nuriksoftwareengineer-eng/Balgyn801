package com.nurba.java.service;

import com.nurba.java.domain.Order;

public interface EmailService {
    void sendRegistrationEmail(String to, String name);
    void sendOrderCreatedEmail(String to, Order order);
    void sendPaymentSuccessEmail(String to, Order order);
    void sendPaymentFailedEmail(String to, Order order);
    void sendOrderShippedEmail(String to, Order order, String trackingNumber);
    void sendOrderDeliveredEmail(String to, Order order);
}
