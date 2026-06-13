package com.nurba.java.service.delivery.provider;

/**
 * Единый интерфейс провайдера доставки. Бизнес-логика магазина (CdekOrderService и пр.)
 * работает только через него и НЕ знает, mock это или реальный API — выбор реализации
 * делается на уровне конфигурации (см. {@code CdekProviderConfig}, {@code cdek.provider}).
 *
 * <p>Реализации:
 * <ul>
 *   <li>{@link MockCdekProvider} — заглушки (детерминированные UUID/трек/URL);</li>
 *   <li>{@link RealCdekProvider} — реальный CDEK API v2 через {@code CdekClient}.</li>
 * </ul>
 */
public interface DeliveryProvider {

    /** true для mock-режима (полезно для админки/диагностики и поля sourcedFromStub). */
    boolean isMock();

    /** Человекочитаемое имя провайдера ("mock-cdek" / "real-cdek"). */
    String name();

    /** Создать отправление у провайдера. Возвращает UUID/трек/документы/сырой ответ. */
    ShipmentResult createShipment(ShipmentRequest request);

    /** Запросить актуальный статус и реквизиты отправления по UUID (синхронизация). */
    ShipmentResult syncStatus(String cdekOrderUuid);

    /** Отменить отправление у провайдера. */
    void cancelShipment(String cdekOrderUuid);
}
