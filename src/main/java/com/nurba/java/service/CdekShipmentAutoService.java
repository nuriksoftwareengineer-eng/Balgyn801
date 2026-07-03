package com.nurba.java.service;

/**
 * Автоматически создаёт отправление СДЭК после подтверждения оплаты.
 * Вызывается из {@code afterCommit}-хука {@code PaymentServiceImpl} чтобы не читать
 * пустую БД до коммита транзакции оплаты.
 */
public interface CdekShipmentAutoService {

    /**
     * Если заказ с данным id имеет тип доставки СДЭК — создаёт отправление
     * через {@link CdekOrderService}. При ошибке логирует и уведомляет
     * администратора в Telegram; исключение не проброшено.
     */
    void triggerIfCdek(Long orderId);
}
