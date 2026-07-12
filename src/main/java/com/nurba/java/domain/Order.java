package com.nurba.java.domain;


import com.nurba.java.enums.DeliveryType;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.enums.ShippingZone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /**
     * Authenticated user who placed the order.
     * Null for anonymous orders (existing behaviour preserved).
     * Used for purchase verification (reviews, order history).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser appUser;
    @Column(precision = 10, scale = 2)
    private BigDecimal totalPrice;

    /** Часть {@link #totalPrice}, приходящая от доставки (СДЭК и т.д.). Может быть null для старых заказов. */
    @Column(precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    // ── Delivery snapshot (computed backend-side at creation, immutable afterwards) ──

    /** Суммарный вес заказа (кг) на момент оформления. */
    @Column(name = "total_weight_kg", precision = 7, scale = 3)
    private BigDecimal totalWeightKg;

    /** Зона доставки, определённая по стране на момент оформления (backend-only). */
    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_zone", length = 20)
    private ShippingZone shippingZone;

    /** Долларовая часть международной доставки (снапшот), null для внутренних/СНГ заказов. */
    @Column(name = "delivery_fee_usd", precision = 10, scale = 2)
    private BigDecimal deliveryFeeUsd;

    /** Курс KZT→USD, использованный для расчёта международной доставки (снапшот, аудит). */
    @Column(name = "exchange_rate_kzt_usd", precision = 12, scale = 4)
    private BigDecimal exchangeRateKztUsd;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    private DeliveryType deliveryType;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    /** Адрес доставки (если не самовывоз); владеющая сторона — {@link DeliveryAddress#order}. */
    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY)
    private DeliveryAddress deliveryAddress;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /** Tracking number assigned by the carrier (CDEK, KazPost, etc.). Set by admin on shipment. */
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
