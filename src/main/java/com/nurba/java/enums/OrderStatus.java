package com.nurba.java.enums;

public enum OrderStatus {
    NEW,            // новый заказ — только что создан
    CONFIRMED,      // подтверждён менеджером
    IN_PRODUCTION,  // в работе — шьют/вышивают
    READY,          // готов к отправке
    SHIPPED,        // отправлен
    DELIVERED,      // доставлен клиенту
    CANCELLED       // отменён
}
