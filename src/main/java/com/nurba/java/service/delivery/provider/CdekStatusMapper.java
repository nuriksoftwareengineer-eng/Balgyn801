package com.nurba.java.service.delivery.provider;

import com.nurba.java.enums.CdekShipmentStatus;

import java.util.Locale;

/**
 * Маппинг кодов статуса СДЭК в нашу модель {@link CdekShipmentStatus}.
 * Используется и реальным провайдером (синхронизация), и webhook-обработчиком.
 * Покрывает требуемые ТЗ статусы (CREATED/ACCEPTED/IN_TRANSIT/ARRIVED/DELIVERED/CANCELED)
 * и распространённые коды реального CDEK API. Неизвестный код → null (статус не меняем).
 */
public final class CdekStatusMapper {

    private CdekStatusMapper() {
    }

    public static CdekShipmentStatus map(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return switch (code.trim().toUpperCase(Locale.ROOT)) {
            case "CREATED" -> CdekShipmentStatus.CREATED;
            case "ACCEPTED",
                 "RECEIVED_AT_SHIPMENT_WAREHOUSE",
                 "ACCEPTED_AT_SENDING_OFFICE",
                 "ACCEPTED_AT_PICK_UP_POINT" -> CdekShipmentStatus.ACCEPTED;
            case "IN_TRANSIT",
                 "ON_WAY",
                 "SENT_TO_TRANSIT_WAREHOUSE",
                 "RETURNED_TO_SENDER" -> CdekShipmentStatus.IN_TRANSIT;
            case "ARRIVED",
                 "RECEIVED_AT_DELIVERY_WAREHOUSE",
                 "READY_FOR_PICKUP",
                 "ARRIVED_AT_PICK_UP_POINT" -> CdekShipmentStatus.ARRIVED;
            case "DELIVERED" -> CdekShipmentStatus.DELIVERED;
            case "RETURNED" -> CdekShipmentStatus.RETURNED;
            case "CANCELED", "CANCELLED", "NOT_DELIVERED", "INVALID" -> CdekShipmentStatus.CANCELLED;
            default -> null;
        };
    }
}
