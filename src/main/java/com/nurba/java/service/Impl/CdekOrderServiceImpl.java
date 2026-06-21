package com.nurba.java.service.Impl;

import com.nurba.java.domain.CdekShipment;
import com.nurba.java.domain.DeliveryAddress;
import com.nurba.java.domain.Order;
import com.nurba.java.dto.responce.CdekShipmentResponse;
import com.nurba.java.enums.CdekShipmentStatus;
import com.nurba.java.enums.DeliveryType;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.CdekMapper;
import com.nurba.java.repositories.CdekShipmentRepository;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.service.CdekOrderService;
import com.nurba.java.service.delivery.provider.DeliveryProvider;
import com.nurba.java.service.delivery.provider.ShipmentRequest;
import com.nurba.java.service.delivery.provider.ShipmentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Создание/синхронизация/отмена отправления через {@link DeliveryProvider}.
 * Провайдер (mock/real) выбирается конфигурацией — этот сервис о нём не знает.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdekOrderServiceImpl implements CdekOrderService {

    private static final int MIN_WEIGHT_GRAMS = 100;

    private final OrderRepository orderRepository;
    private final CdekShipmentRepository shipmentRepository;
    private final DeliveryProvider provider;
    private final CdekMapper cdekMapper;

    @Override
    @Transactional
    public CdekShipmentResponse createShipment(Long orderId) {
        Order order = requireOrder(orderId);
        if (order.getDeliveryType() != DeliveryType.CDEK) {
            throw new BusinessRuleException(
                    "Отправление СДЭК доступно только для заказов с доставкой СДЭК");
        }
        DeliveryAddress address = order.getDeliveryAddress();
        if (address == null) {
            throw new BusinessRuleException("У заказа нет адреса доставки");
        }

        CdekShipment shipment = shipmentRepository.findByOrder_Id(orderId)
                .orElseGet(() -> {
                    CdekShipment s = new CdekShipment();
                    s.setOrder(order);
                    s.setCreatedAt(LocalDateTime.now());
                    return s;
                });

        ShipmentRequest request = new ShipmentRequest(
                order.getId(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getCity(),
                address.getCityCode(),
                address.getPvzCode(),
                address.getStreet(),
                shipment.getTariffCode(),
                toGrams(order.getTotalWeightKg()),
                order.getDeliveryFee()
        );

        ShipmentResult result = provider.createShipment(request);

        shipment.setCdekOrderUuid(result.cdekOrderUuid());
        shipment.setTrackingNumber(result.trackingNumber());
        shipment.setStatus(result.status() != null ? result.status() : CdekShipmentStatus.CREATED);
        shipment.setCdekDeliveryMode(result.deliveryMode());
        shipment.setEstimatedDeliveryDate(result.estimatedDeliveryDate());
        shipment.setInvoiceUrl(result.invoiceUrl());
        shipment.setBarcodeUrl(result.barcodeUrl());
        shipment.setRawResponse(result.rawResponse());
        shipment.setDeliveryPrice(order.getDeliveryFee());
        shipment.setDeliveryPointCode(address.getPvzCode());
        shipment.setDeliveryPointAddress(address.getStreet());
        shipment.setToCity(address.getCity());
        shipment.setUpdatedAt(LocalDateTime.now());

        return toResponse(shipmentRepository.save(shipment));
    }

    @Override
    @Transactional
    public CdekShipmentResponse syncStatus(Long orderId) {
        CdekShipment shipment = requireShipment(orderId);
        requireUuid(shipment);
        ShipmentResult result = provider.syncStatus(shipment.getCdekOrderUuid());
        if (result.status() != null) {
            shipment.setStatus(result.status());
        }
        if (result.trackingNumber() != null) {
            shipment.setTrackingNumber(result.trackingNumber());
        }
        if (result.estimatedDeliveryDate() != null) {
            shipment.setEstimatedDeliveryDate(result.estimatedDeliveryDate());
        }
        if (result.rawResponse() != null) {
            shipment.setRawResponse(result.rawResponse());
        }
        shipment.setUpdatedAt(LocalDateTime.now());
        return toResponse(shipmentRepository.save(shipment));
    }

    @Override
    @Transactional
    public CdekShipmentResponse cancelShipment(Long orderId) {
        CdekShipment shipment = requireShipment(orderId);
        requireUuid(shipment);
        provider.cancelShipment(shipment.getCdekOrderUuid());
        shipment.setStatus(CdekShipmentStatus.CANCELLED);
        shipment.setUpdatedAt(LocalDateTime.now());
        return toResponse(shipmentRepository.save(shipment));
    }

    /** Маппинг + признак mock по префиксу UUID (детерминирован для mock-провайдера). */
    private CdekShipmentResponse toResponse(CdekShipment shipment) {
        CdekShipmentResponse resp = cdekMapper.toResponse(shipment);
        resp.setMock(shipment.getCdekOrderUuid() != null
                && shipment.getCdekOrderUuid().startsWith("MOCK-"));
        return resp;
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private Order requireOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: id=" + orderId));
    }

    private CdekShipment requireShipment(Long orderId) {
        return shipmentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Отправление СДЭК не создано для заказа id=" + orderId));
    }

    private void requireUuid(CdekShipment shipment) {
        if (shipment.getCdekOrderUuid() == null || shipment.getCdekOrderUuid().isBlank()) {
            throw new BusinessRuleException(
                    "У отправления нет UUID СДЭК — сначала создайте отправление");
        }
    }

    private int toGrams(BigDecimal weightKg) {
        if (weightKg == null) {
            return MIN_WEIGHT_GRAMS;
        }
        int grams = weightKg.multiply(BigDecimal.valueOf(1000))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        return Math.max(grams, MIN_WEIGHT_GRAMS);
    }
}
