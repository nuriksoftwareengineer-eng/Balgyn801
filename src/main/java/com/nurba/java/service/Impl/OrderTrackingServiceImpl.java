package com.nurba.java.service.Impl;

import com.nurba.java.domain.Order;
import com.nurba.java.domain.OrderHistory;
import com.nurba.java.dto.responce.CdekShipmentResponse;
import com.nurba.java.dto.responce.OrderStatusHistoryEntry;
import com.nurba.java.dto.responce.OrderTrackingResponse;
import com.nurba.java.repositories.OrderHistoryRepository;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.service.CdekShipmentService;
import com.nurba.java.service.OrderTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderTrackingServiceImpl implements OrderTrackingService {

    private final OrderRepository orderRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final CdekShipmentService cdekShipmentService;

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponse getForGuest(Long orderId, String phone) {
        Order order = findOrder(orderId);
        if (!phoneMatches(order, phone)) {
            // Return 404 — never reveal whether the order exists (prevents IDOR enumeration)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        OrderTrackingResponse response = buildResponse(order);
        // Strip admin-only document URLs for guest callers
        if (response.getCdekShipment() != null) {
            response.getCdekShipment().setInvoiceUrl(null);
            response.getCdekShipment().setBarcodeUrl(null);
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponse getForUser(Long orderId, String userEmail) {
        Order order = findOrder(orderId);
        if (order.getAppUser() == null ||
                !order.getAppUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return buildResponse(order);
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private boolean phoneMatches(Order order, String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) return false;
        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.isEmpty()) return false;

        if (order.getCustomer() != null && order.getCustomer().getPhone() != null) {
            if (order.getCustomer().getPhone().replaceAll("\\D", "").equals(digits)) return true;
        }
        if (order.getDeliveryAddress() != null &&
                order.getDeliveryAddress().getRecipientPhone() != null) {
            if (order.getDeliveryAddress().getRecipientPhone().replaceAll("\\D", "").equals(digits)) return true;
        }
        return false;
    }

    private OrderTrackingResponse buildResponse(Order order) {
        List<OrderHistory> history =
                orderHistoryRepository.findByOrder_IdOrderByDateAddedDesc(order.getId());

        CdekShipmentResponse cdek = cdekShipmentService.getByOrder(order.getId());

        OrderTrackingResponse response = new OrderTrackingResponse();
        response.setOrderId(order.getId());
        response.setOrderStatus(order.getStatus());
        response.setDeliveryType(order.getDeliveryType());
        response.setTotalPrice(order.getTotalPrice());
        response.setCreatedAt(order.getCreatedAt());
        response.setStatusHistory(
                history.stream()
                        .map(h -> new OrderStatusHistoryEntry(h.getStatus(), h.getDateAdded()))
                        .collect(Collectors.toList())
        );
        response.setCdekShipment(cdek);

        // Consolidated tracking number: cdekShipment wins, then order-level field
        String trackingNumber = null;
        if (cdek != null && cdek.getTrackingNumber() != null && !cdek.getTrackingNumber().isBlank()) {
            trackingNumber = cdek.getTrackingNumber();
        } else if (order.getTrackingNumber() != null && !order.getTrackingNumber().isBlank()) {
            trackingNumber = order.getTrackingNumber();
        }
        response.setTrackingNumber(trackingNumber);

        return response;
    }
}
