package com.nurba.java.service;

import com.nurba.java.dto.delivery.CdekWebhookRequest;

/**
 * Обработка вебхуков CDEK: маппинг входящего статуса в нашу модель и сохранение в
 * {@code cdek_shipments}. Работает уже сейчас на тестовых payload (mock-сценарий).
 */
public interface CdekWebhookService {

    /**
     * Обработать событие вебхука. Идемпотентна и устойчива: если отправление по UUID не найдено
     * или статус неизвестен — событие игнорируется без ошибки (CDEK ретраит вебхуки).
     *
     * @return true, если статус отправления был обновлён.
     */
    boolean handle(CdekWebhookRequest request);
}
