package com.nurba.java.service;

import com.nurba.java.domain.Order;

public interface TelegramNotificationService {
    void notifyNewOrder(Order order);
    void notifyNewOrderById(Long orderId);
    void notifyPaymentSuccess(Order order);
    void notifyPaymentFailed(Order order);
    void notifyOrderShipped(Order order);
    void notifyOrderDelivered(Order order);
    void notifyNewUser(String email);
    void notifyError(String context, String message);
}
