package com.nurba.java.service.Impl;

import com.nurba.java.domain.CdekShipment;
import com.nurba.java.dto.delivery.CdekWebhookRequest;
import com.nurba.java.enums.CdekShipmentStatus;
import com.nurba.java.repositories.CdekShipmentRepository;
import com.nurba.java.service.CdekWebhookService;
import com.nurba.java.service.delivery.provider.CdekStatusMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Обработчик вебхуков CDEK. Находит отправление по UUID, маппит код статуса через
 * {@link CdekStatusMapper} и обновляет {@code cdek_shipments}. Не бросает исключений на
 * неизвестных событиях — просто игнорирует (CDEK ретраит).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdekWebhookServiceImpl implements CdekWebhookService {

    private final CdekShipmentRepository shipmentRepository;

    @Override
    @Transactional
    public boolean handle(CdekWebhookRequest request) {
        if (request == null || request.uuid() == null || request.uuid().isBlank()) {
            log.warn("CDEK webhook: пустой uuid, событие проигнорировано");
            return false;
        }
        CdekShipment shipment = shipmentRepository.findAll().stream()
                .filter(s -> request.uuid().equals(s.getCdekOrderUuid()))
                .findFirst()
                .orElse(null);
        if (shipment == null) {
            log.warn("CDEK webhook: отправление с uuid={} не найдено", request.uuid());
            return false;
        }

        String code = request.attributes() != null
                ? (request.attributes().code() != null
                ? request.attributes().code()
                : request.attributes().statusCode())
                : null;
        CdekShipmentStatus mapped = CdekStatusMapper.map(code);

        boolean changed = false;
        if (mapped != null) {
            shipment.setStatus(mapped);
            changed = true;
        }
        if (request.attributes() != null && request.attributes().cdekNumber() != null
                && !request.attributes().cdekNumber().isBlank()) {
            shipment.setTrackingNumber(request.attributes().cdekNumber());
            changed = true;
        }
        if (changed) {
            shipment.setUpdatedAt(LocalDateTime.now());
            shipmentRepository.save(shipment);
            log.info("CDEK webhook: uuid={} code={} -> status={}",
                    request.uuid(), code, mapped);
        } else {
            log.info("CDEK webhook: uuid={} code={} — статус не изменён", request.uuid(), code);
        }
        return changed;
    }
}
