package com.nurba.java.enums;

public enum CdekShipmentStatus {
    CREATED,     // заказ создан у нас, ещё не передан в СДЭК
    ACCEPTED,    // СДЭК принял заказ
    IN_TRANSIT,  // в пути
    DELIVERED,   // доставлен
    RETURNED,    // возврат
    CANCELLED    // отменён в СДЭК
}
