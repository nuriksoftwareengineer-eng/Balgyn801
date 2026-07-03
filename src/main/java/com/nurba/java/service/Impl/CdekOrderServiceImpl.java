package com.nurba.java.service.Impl;

import com.nurba.java.domain.CdekShipment;
import com.nurba.java.domain.DeliveryAddress;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.OrderItem;
import com.nurba.java.dto.responce.CdekShipmentResponse;
import com.nurba.java.enums.CdekShipmentStatus;
import com.nurba.java.enums.DeliveryType;
import com.nurba.java.domain.GarmentProfile;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.CdekMapper;
import com.nurba.java.repositories.CdekShipmentRepository;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.service.CdekOrderService;
import com.nurba.java.service.delivery.provider.DeliveryProvider;
import com.nurba.java.service.delivery.provider.OrderItemSnapshot;
import com.nurba.java.service.delivery.provider.ShipmentRequest;
import com.nurba.java.service.delivery.provider.ShipmentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

        // Build per-item snapshots with real weights and prices
        List<OrderItemSnapshot> itemSnapshots = buildItemSnapshots(order.getOrderItems());
        int totalWeightGrams = computeTotalWeightGrams(itemSnapshots);
        int[] dims = computePackageDimensions(order.getOrderItems()); // [maxLength, maxWidth, sumHeight]

        // Recipient email — from authenticated user if available
        String recipientEmail = (order.getAppUser() != null)
                ? order.getAppUser().getEmail()
                : null;

        ShipmentRequest request = new ShipmentRequest(
                order.getId(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                recipientEmail,
                address.getCity(),
                address.getCityCode(),
                address.getPvzCode(),
                address.getStreet(),
                shipment.getTariffCode(),
                totalWeightGrams,
                dims[0],
                dims[1],
                dims[2],
                order.getDeliveryFee(),
                itemSnapshots
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

    // ── helpers ───────────────────────────────────────────────────────────────────

    /**
     * Строит список снимков позиций заказа.
     * Дизайн-позиции: имя дизайна + тип изделия, вес из GarmentWeightService.
     * Устаревшие product-позиции: только если designGarment == null, используют дефолт 500 г.
     */
    private List<OrderItemSnapshot> buildItemSnapshots(List<OrderItem> items) {
        List<OrderItemSnapshot> result = new ArrayList<>();
        if (items == null) return result;
        for (OrderItem item : items) {
            int qty = (item.getQuantity() == null || item.getQuantity() < 1) ? 1 : item.getQuantity();
            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;

            if (item.getDesignGarment() != null) {
                DesignGarment dg = item.getDesignGarment();
                String designName = (dg.getDesign() != null && dg.getDesign().getName() != null)
                        ? dg.getDesign().getName()
                        : "Дизайн";
                GarmentProfile profile = dg.getGarmentProfile();
                String garmentLabel = profile != null ? profile.getName() : "Изделие";
                String name = designName + " — " + garmentLabel;
                String wareKey = "DG-" + dg.getId();
                int weightGrams = toGrams(profile != null ? profile.getWeightKg() : new BigDecimal("0.5"));
                result.add(new OrderItemSnapshot(name, wareKey, price, weightGrams, qty));

            } else if (item.getProduct() != null) {
                String name = item.getProduct().getTitle() != null
                        ? item.getProduct().getTitle() : "Товар";
                String wareKey = "P-" + item.getProduct().getId();
                result.add(new OrderItemSnapshot(name, wareKey, price, 500, qty));
            }
        }
        if (result.isEmpty()) {
            // Fallback: one synthetic line so CDEK always gets at least one item
            result.add(new OrderItemSnapshot("Товар", "BALGYN-1", BigDecimal.ZERO, MIN_WEIGHT_GRAMS, 1));
        }
        return result;
    }

    /** Суммарный вес всех позиций (weightGrams × quantity), не менее MIN_WEIGHT_GRAMS. */
    private int computeTotalWeightGrams(List<OrderItemSnapshot> snapshots) {
        int total = snapshots.stream().mapToInt(OrderItemSnapshot::totalWeightGrams).sum();
        return Math.max(total, MIN_WEIGHT_GRAMS);
    }

    /**
     * Вычисляет габариты посылки по позициям заказа:
     * длина = MAX(lengthCm), ширина = MAX(widthCm), высота = SUM(heightCm × qty).
     * Минимальное значение каждого измерения — 1 см (CDEK отклоняет 0).
     */
    private int[] computePackageDimensions(List<OrderItem> items) {
        int maxLength = 1, maxWidth = 1, sumHeight = 0;
        if (items != null) {
            for (OrderItem item : items) {
                if (item.getDesignGarment() == null) continue;
                GarmentProfile profile = item.getDesignGarment().getGarmentProfile();
                if (profile == null) continue;
                int qty = (item.getQuantity() == null || item.getQuantity() < 1) ? 1 : item.getQuantity();
                maxLength = Math.max(maxLength, profile.getLengthCm());
                maxWidth  = Math.max(maxWidth,  profile.getWidthCm());
                sumHeight += profile.getHeightCm() * qty;
            }
        }
        return new int[]{ maxLength, maxWidth, Math.max(sumHeight, 1) };
    }

    private static int toGrams(BigDecimal weightKg) {
        if (weightKg == null) return MIN_WEIGHT_GRAMS;
        int g = weightKg.multiply(BigDecimal.valueOf(1000))
                .setScale(0, RoundingMode.HALF_UP).intValue();
        return Math.max(g, 1);
    }

    private CdekShipmentResponse toResponse(CdekShipment shipment) {
        CdekShipmentResponse resp = cdekMapper.toResponse(shipment);
        resp.setMock(shipment.getCdekOrderUuid() != null
                && shipment.getCdekOrderUuid().startsWith("MOCK-"));
        return resp;
    }

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
}
