package com.nurba.java.enums;

public enum DeliveryType {
    PICKUP,        // самовывоз (Казахстан, бесплатно) — клиенту показывается как "Pickup"
    TAXI,          // доставка по Казахстану (фикс. тариф) — клиенту показывается как "Delivery"
    CDEK,          // СДЭК (СНГ)
    POSTAL,        // почтовая доставка (СНГ)
    INTERNATIONAL  // международная доставка — клиенту показывается как "International Shipping"
}
