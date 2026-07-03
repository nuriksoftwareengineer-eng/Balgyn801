package com.nurba.java.service.delivery.provider;

import com.nurba.java.enums.CdekShipmentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Mock-провайдер CDEK: детерминированные UUID/трек/документы без обращения к сети.
 * Позволяет полностью пройти сценарий создания отправления/синхронизации/отмены до получения
 * ключей. После получения ключей переключение на {@link RealCdekProvider} — только через
 * конфигурацию ({@code cdek.provider}); бизнес-логика не меняется.
 */
@Slf4j
@Component
public class MockCdekProvider implements DeliveryProvider {

    @Override
    public boolean isMock() {
        return true;
    }

    @Override
    public String name() {
        return "mock-cdek";
    }

    @Override
    public ShipmentResult createShipment(ShipmentRequest request) {
        String uuid = "MOCK-" + UUID.randomUUID();
        long base = request.orderId() == null ? 0 : request.orderId();
        String track = "MOCK" + String.format("%010d", Math.abs(base * 31 + (uuid.hashCode() % 100000)));
        log.info("CDEK mock: создано отправление uuid={} track={} order={}", uuid, track, request.orderId());
        return new ShipmentResult(
                uuid,
                track,
                CdekShipmentStatus.CREATED,
                request.pvzCode() != null ? "warehouse-pvz" : "warehouse-door",
                LocalDate.now().plusDays(4),
                mockDocUrl("invoice", uuid),
                mockDocUrl("barcode", uuid),
                mockRawCreate(uuid, track, request)
        );
    }

    @Override
    public ShipmentResult syncStatus(String cdekOrderUuid) {
        // Mock: считаем, что СДЭК уже принял отправление.
        log.info("CDEK mock: синхронизация статуса uuid={}", cdekOrderUuid);
        return new ShipmentResult(
                cdekOrderUuid,
                null,
                CdekShipmentStatus.ACCEPTED,
                null,
                LocalDate.now().plusDays(3),
                mockDocUrl("invoice", cdekOrderUuid),
                mockDocUrl("barcode", cdekOrderUuid),
                "{\"mock\":true,\"op\":\"sync\",\"uuid\":\"" + cdekOrderUuid
                        + "\",\"status\":\"ACCEPTED\"}"
        );
    }

    @Override
    public void cancelShipment(String cdekOrderUuid) {
        log.info("CDEK mock: отмена отправления uuid={}", cdekOrderUuid);
        // no-op в mock-режиме
    }

    private static String mockDocUrl(String kind, String uuid) {
        return "https://mock.cdek.local/print/" + kind + "/" + uuid + ".pdf";
    }

    private static String mockRawCreate(String uuid, String track, ShipmentRequest r) {
        int itemCount = r.items() != null ? r.items().size() : 0;
        return "{\"mock\":true,\"op\":\"create\",\"entity\":{\"uuid\":\"" + uuid
                + "\",\"cdek_number\":\"" + track + "\"},\"order_id\":" + r.orderId()
                + ",\"pvz\":\"" + (r.pvzCode() == null ? "" : r.pvzCode())
                + "\",\"weight_grams\":" + r.weightGrams()
                + ",\"item_count\":" + itemCount + "}";
    }
}
