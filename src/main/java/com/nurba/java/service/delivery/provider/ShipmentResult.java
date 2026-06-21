package com.nurba.java.service.delivery.provider;

import com.nurba.java.enums.CdekShipmentStatus;

import java.time.LocalDate;

/**
 * Результат операции провайдера над отправлением (создание/синхронизация).
 * Сохраняется в {@code cdek_shipments}. {@code rawResponse} — полный сырой ответ для аудита
 * (в mock-режиме — mock-payload).
 *
 * @param cdekOrderUuid        UUID заказа в СДЭК
 * @param trackingNumber       трек-номер (может появиться позже создания — тогда null)
 * @param status               маппинг статуса в нашу модель
 * @param deliveryMode         режим доставки СДЭК (например "warehouse-pvz")
 * @param estimatedDeliveryDate ориентировочная дата доставки (если известна)
 * @param invoiceUrl           ссылка на накладную (PDF)
 * @param barcodeUrl           ссылка на штрихкод/ярлык (PDF)
 * @param rawResponse          полный сырой ответ провайдера
 */
public record ShipmentResult(
        String cdekOrderUuid,
        String trackingNumber,
        CdekShipmentStatus status,
        String deliveryMode,
        LocalDate estimatedDeliveryDate,
        String invoiceUrl,
        String barcodeUrl,
        String rawResponse
) {
}
