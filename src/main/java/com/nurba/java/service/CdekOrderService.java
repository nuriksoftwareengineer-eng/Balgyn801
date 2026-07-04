package com.nurba.java.service;

import com.nurba.java.dto.responce.CdekShipmentResponse;

/**
 * Жизненный цикл отправления СДЭК для заказа. Работает через {@code DeliveryProvider}
 * (mock/real — по конфигурации) и сохраняет результат в {@code cdek_shipments}.
 * Вызывается из админки и (в перспективе) автоматически после успешной оплаты.
 */
public interface CdekOrderService {

    /**
     * Создать (или пересоздать — «повторить создание») отправление по заказу.
     * Сохраняет UUID, трек-номер, документы и сырой ответ провайдера.
     */
    CdekShipmentResponse createShipment(Long orderId);

    /** Синхронизировать статус/трек отправления с провайдером. */
    CdekShipmentResponse syncStatus(Long orderId);

    /** Отменить отправление у провайдера и пометить статус CANCELLED. */
    CdekShipmentResponse cancelShipment(Long orderId);

    /** Получить (или обновить) URL документов (штрихкод, квитанция) у провайдера CDEK. */
    CdekShipmentResponse fetchDocuments(Long orderId);
}
