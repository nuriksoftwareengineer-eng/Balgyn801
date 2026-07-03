package com.nurba.java.service.Impl;

import com.nurba.java.domain.Order;
import com.nurba.java.enums.DeliveryType;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.service.CdekOrderService;
import com.nurba.java.service.CdekShipmentAutoService;
import com.nurba.java.service.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CdekShipmentAutoServiceImpl implements CdekShipmentAutoService {

    private final OrderRepository orderRepository;
    private final CdekOrderService cdekOrderService;
    private final TelegramNotificationService telegramNotificationService;

    @Override
    @Async
    @Transactional
    public void triggerIfCdek(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("[CDEK-AUTO] Order #{} not found, skipping", orderId);
            return;
        }
        if (order.getDeliveryType() != DeliveryType.CDEK) {
            return;
        }
        try {
            log.info("[CDEK-AUTO] Creating shipment for order #{}", orderId);
            cdekOrderService.createShipment(orderId);
            log.info("[CDEK-AUTO] Shipment created successfully for order #{}", orderId);
        } catch (Exception ex) {
            log.error("[CDEK-AUTO] Failed to create shipment for order #{}: {}", orderId, ex.getMessage(), ex);
            telegramNotificationService.notifyError(
                    "CDEK auto-shipment",
                    "Не удалось создать отправление СДЭК для заказа #" + orderId
                    + ". Создайте вручную в панели администратора.\n"
                    + "Ошибка: " + ex.getMessage()
            );
        }
    }
}
