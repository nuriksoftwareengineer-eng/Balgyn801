package com.nurba.java.enums;

public enum CdekShipmentStatus {
    CREATED,     // заказ создан у нас / в СДЭК, ещё не принят
    ACCEPTED,    // СДЭК принял заказ
    IN_TRANSIT,  // в пути
    ARRIVED,     // прибыл в пункт выдачи / город получателя
    DELIVERED,   // доставлен
    RETURNED,    // возврат
    CANCELLED    // отменён в СДЭК (CDEK "CANCELED")
}
