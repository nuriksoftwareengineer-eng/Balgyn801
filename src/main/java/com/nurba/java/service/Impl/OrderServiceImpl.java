package com.nurba.java.service.Impl;

import com.nurba.java.domain.AppUser;
import com.nurba.java.domain.Color;
import com.nurba.java.domain.Customer;
import com.nurba.java.domain.DeliveryAddress;
import com.nurba.java.domain.OrderHistory;
import com.nurba.java.domain.UserAddress;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.DesignGarmentPrice;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.OrderItem;
import com.nurba.java.domain.Product;
import com.nurba.java.domain.Size;
import com.nurba.java.dto.request.CreateOrderRequest;
import com.nurba.java.dto.request.DeliveryAddressRequest;
import com.nurba.java.dto.request.OrderItemRequest;
import com.nurba.java.dto.request.UpdateOrderStatusRequest;
import com.nurba.java.dto.responce.OrderResponse;
import com.nurba.java.enums.Currency;
import com.nurba.java.enums.DeliveryType;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.DeliveryMapper;
import com.nurba.java.mapper.OrderMapper;
import com.nurba.java.model.ProductColorOption;
import com.nurba.java.domain.Inventory;
import com.nurba.java.repositories.AppUserRepository;
import com.nurba.java.repositories.ColorRepository;
import com.nurba.java.repositories.InventoryRepository;
import com.nurba.java.repositories.OrderHistoryRepository;
import com.nurba.java.service.CouponService;
import com.nurba.java.enums.DiscountType;
import com.nurba.java.repositories.UserAddressRepository;
import com.nurba.java.repositories.CustomerRepository;
import com.nurba.java.repositories.DeliveryAddressRepository;
import com.nurba.java.repositories.DesignGarmentPriceRepository;
import com.nurba.java.repositories.DesignGarmentRepository;
import com.nurba.java.repositories.OrderItemRepository;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.repositories.ProductRepository;
import com.nurba.java.repositories.SizeRepository;
import com.nurba.java.service.CdekShipmentService;
import com.nurba.java.service.EmailService;
import com.nurba.java.service.GarmentWeightService;
import com.nurba.java.service.OrderService;
import com.nurba.java.service.TelegramNotificationService;
import com.nurba.java.service.delivery.DeliveryPricingService;
import com.nurba.java.service.delivery.DeliveryQuote;
import com.nurba.java.service.Impl.OrderExpiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final OrderMapper orderMapper;
    private final DeliveryMapper deliveryMapper;
    private final DeliveryPricingService deliveryPricingService;
    private final GarmentWeightService garmentWeightService;

    // ── Catalog repositories (design-based order support) ─────────────────────
    private final DesignGarmentRepository designGarmentRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final DesignGarmentPriceRepository designGarmentPriceRepository;

    // ── Inventory (stock check for design-based items) ────────────────────────
    private final InventoryRepository inventoryRepository;

    // ── User repository (purchase tracking) + saved addresses ─────────────────
    private final AppUserRepository appUserRepository;
    private final UserAddressRepository userAddressRepository;

    // ── Order history (audit log of status changes) ────────────────────────────
    private final OrderHistoryRepository orderHistoryRepository;

    private final OrderExpiryService orderExpiryService;
    private final CouponService couponService;
    private final EmailService emailService;
    private final TelegramNotificationService telegramNotificationService;
    private final CdekShipmentService cdekShipmentService;


    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        validateCreateOrderRequest(request);

        Customer customer = createCustomer(request);

        Order order = new Order();
        order.setCustomer(customer);
        order.setAppUser(resolveAuthenticatedUser());   // null for anonymous orders
        order.setDeliveryType(request.getDeliveryType());
        // Orders start unpaid. Inventory is reserved (deducted under lock) here, but the order
        // stays hidden from admin until a successful payment advances it to CONFIRMED. If payment
        // never arrives, OrderExpiryService releases the inventory and marks it EXPIRED.
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setComment(request.getComment());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setTotalPrice(BigDecimal.ZERO);

        Order savedOrder = orderRepository.save(order);

        BigDecimal itemsTotal = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            int quantity = itemRequest.getQuantity() != null ? itemRequest.getQuantity() : 0;
            if (quantity <= 0) {
                throw new BusinessRuleException("Количество должно быть больше 0");
            }

            BigDecimal lineTotal;
            if (itemRequest.getDesignGarmentId() != null) {
                lineTotal = buildDesignOrderItem(savedOrder, itemRequest, quantity);
            } else {
                lineTotal = buildProductOrderItem(savedOrder, itemRequest, quantity);
            }
            itemsTotal = itemsTotal.add(lineTotal);
        }

        // ── Backend-computed delivery — the frontend is never trusted for fee/weight/zone/method ──
        // Resolve the address first; CDEK needs the destination city for its tariff.
        DeliveryAddressRequest addrReq = null;
        if (requiresDeliveryAddress(request.getDeliveryType())) {
            // resolveDeliveryAddress returns either the inline address or a snapshot
            // of the saved UserAddress — both paths produce a plain DeliveryAddressRequest.
            addrReq = resolveDeliveryAddress(request);
            if (addrReq == null) {
                throw new BusinessRuleException("Укажите адрес доставки для выбранного способа получения");
            }
            validateDeliveryAddress(addrReq);
        }

        // Total weight is computed from the persisted order items (backend-only).
        List<OrderItem> savedItems = orderItemRepository.findByOrder_Id(savedOrder.getId());
        BigDecimal weightKg = garmentWeightService.calculateOrderWeight(savedItems);

        // The backend computes the fee, zone, and (for international) USD + rate. Nothing here
        // comes from the client — removing the only delivery-cost manipulation vector.
        DeliveryQuote quote = deliveryPricingService.quote(
                request.getDeliveryType(), request.getCountryIso2(), addrReq, weightKg);
        BigDecimal deliveryFee = quote.feeKzt();

        // CDEK: доставка оплачивается при получении в ПВЗ — не включаем в сумму заказа на сайте
        if (request.getDeliveryType() == DeliveryType.CDEK) {
            deliveryFee = BigDecimal.ZERO;
        }

        BigDecimal grandTotal = itemsTotal.add(deliveryFee);

        // Apply coupon if provided
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            try {
                com.nurba.java.domain.Coupon coupon = couponService.findValidCoupon(request.getCouponCode(), grandTotal);
                BigDecimal discount = BigDecimal.ZERO;
                if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
                    discount = grandTotal.multiply(coupon.getDiscountValue())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                } else {
                    discount = coupon.getDiscountValue().min(grandTotal);
                }
                grandTotal = grandTotal.subtract(discount).max(BigDecimal.ZERO);
                savedOrder.setCouponCode(coupon.getCode());
                savedOrder.setDiscountAmount(discount);
                couponService.incrementUsage(coupon.getId());
            } catch (com.nurba.java.exception.BusinessRuleException e) {
                throw new com.nurba.java.exception.BusinessRuleException("Промокод недействителен: " + e.getMessage());
            }
        }

        savedOrder.setDeliveryFee(deliveryFee.signum() > 0 ? deliveryFee : null);
        savedOrder.setTotalPrice(grandTotal);
        savedOrder.setTotalWeightKg(quote.weightKg());
        savedOrder.setShippingZone(quote.zone());
        savedOrder.setDeliveryFeeUsd(quote.feeUsd());
        savedOrder.setExchangeRateKztUsd(quote.exchangeRateKztUsd());
        savedOrder.setUpdatedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(savedOrder);

        if (addrReq != null) {
            DeliveryAddress address = deliveryMapper.toEntity(addrReq);
            address.setOrder(updatedOrder);
            // Snapshot backend-resolved delivery metadata onto the immutable address record.
            address.setCountryIso2(normalizeIso2(request.getCountryIso2()));
            address.setPvzCode(trimToNull(request.getPvzCode()));
            address.setCityCode(quote.cdekCityCode());
            deliveryAddressRepository.save(address);
        }

        recordHistory(updatedOrder, OrderStatus.PENDING_PAYMENT);

        if (updatedOrder.getAppUser() != null) {
            emailService.sendOrderCreatedEmail(updatedOrder.getAppUser().getEmail(), updatedOrder);
        }
        final long notifyOrderId = updatedOrder.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                telegramNotificationService.notifyNewOrderById(notifyOrderId);
            }
        });

        Order withRelations = orderRepository.findById(updatedOrder.getId())
                .orElseThrow(() -> new NotFoundException("Заказ не найден после создания"));
        return orderMapper.toResponse(withRelations);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Product-based order item (existing logic, extracted to private method)
    // ─────────────────────────────────────────────────────────────────────────

    private BigDecimal buildProductOrderItem(Order order, OrderItemRequest itemRequest, int quantity) {
        Product product = productRepository.findById(itemRequest.getProductId())
                .orElseThrow(() -> new NotFoundException("Товар не найден"));

        if (Boolean.FALSE.equals(product.getInStock())) {
            throw new BusinessRuleException("Товар нет в наличии");
        }

        validateProductVariant(product, itemRequest);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(product.getPrice());
        orderItem.setSizeLabel(resolveSizeLabel(product, itemRequest));
        orderItem.setColorName(resolveColorName(product, itemRequest));

        orderItemRepository.save(orderItem);

        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Design-based order item (new catalog path)
    // ─────────────────────────────────────────────────────────────────────────

    private BigDecimal buildDesignOrderItem(Order order, OrderItemRequest itemRequest, int quantity) {
        DesignGarment garment = designGarmentRepository.findById(itemRequest.getDesignGarmentId())
                .orElseThrow(() -> new NotFoundException(
                        "Вариант дизайна не найден: " + itemRequest.getDesignGarmentId()));

        if (Boolean.FALSE.equals(garment.getActive())) {
            throw new BusinessRuleException("Вариант дизайна недоступен");
        }

        // Resolve currency (default KZT)
        Currency currency = resolveCurrency(itemRequest.getCurrency());

        // Validate color belongs to this garment
        if (itemRequest.getColorId() == null) {
            throw new BusinessRuleException("Укажите цвет для варианта дизайна");
        }
        Color color = colorRepository.findById(itemRequest.getColorId())
                .orElseThrow(() -> new NotFoundException("Цвет не найден: " + itemRequest.getColorId()));
        boolean colorAssigned = garment.getColors().stream()
                .anyMatch(c -> c.getId().equals(color.getId()));
        if (!colorAssigned) {
            throw new BusinessRuleException(
                    "Цвет «" + color.getName() + "» недоступен для данного варианта");
        }

        // Validate size belongs to this garment
        if (itemRequest.getSizeId() == null) {
            throw new BusinessRuleException("Укажите размер для варианта дизайна");
        }
        Size size = sizeRepository.findById(itemRequest.getSizeId())
                .orElseThrow(() -> new NotFoundException("Размер не найден: " + itemRequest.getSizeId()));
        boolean sizeAssigned = garment.getSizes().stream()
                .anyMatch(s -> s.getId().equals(size.getId()));
        if (!sizeAssigned) {
            throw new BusinessRuleException(
                    "Размер «" + size.getLabel() + "» недоступен для данного варианта");
        }

        // ── Inventory: lock the row, verify stock, and immediately deduct ──
        // SELECT FOR UPDATE prevents concurrent orders from overselling.
        // Thread B blocks here until Thread A's transaction commits, then reads
        // the already-decremented quantity and fails the check if stock is exhausted.
        Inventory inventory = inventoryRepository
                .findAndLockByGarmentColorSize(garment.getId(), color.getId(), size.getId())
                .orElseThrow(() -> new BusinessRuleException("Товар отсутствует на складе"));
        if (inventory.getQuantity() < quantity) {
            throw new BusinessRuleException(
                    "Недостаточно товара на складе: доступно " + inventory.getQuantity()
                            + ", запрошено " + quantity);
        }
        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);

        // Look up price for chosen currency
        DesignGarmentPrice price = designGarmentPriceRepository
                .findByDesignGarment_IdAndCurrency(garment.getId(), currency)
                .orElseThrow(() -> new BusinessRuleException(
                        "Цена в валюте " + currency.name() + " не задана для данного варианта"));

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setDesignGarment(garment);
        orderItem.setColor(color);
        orderItem.setSize(size);
        orderItem.setCurrency(currency);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(price.getAmount());
        // Populate plain-string fields so existing response rendering works for both paths
        orderItem.setSizeLabel(size.getLabel());
        orderItem.setColorName(color.getName());

        orderItemRepository.save(orderItem);

        return price.getAmount().multiply(BigDecimal.valueOf(quantity));
    }

    private static Currency resolveCurrency(String raw) {
        if (raw == null || raw.isBlank()) {
            return Currency.KZT;
        }
        try {
            return Currency.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Неизвестная валюта: " + raw);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Existing helpers (unchanged)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + id));

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAll() {
        // Admin panel must never show unpaid (PENDING_PAYMENT) or abandoned (EXPIRED) orders.
        return orderRepository
                .findByStatusNotInOrderByCreatedAtDesc(
                        List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.EXPIRED))
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + id));
        OrderStatus next = request.getStatus();
        assertAllowedStatusTransition(order.getStatus(), next);

        // When admin cancels an order: release reserved inventory (only if goods haven't shipped yet)
        // and cancel any PENDING payments so no dangling records remain.
        if (next == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            // Goods in transit (SHIPPED) or delivered have already consumed physical stock;
            // returning them to available inventory would create phantom stock.
            boolean goodsStillInHouse = order.getStatus() != OrderStatus.SHIPPED
                    && order.getStatus() != OrderStatus.DELIVERED;
            if (goodsStillInHouse) {
                orderExpiryService.releaseInventory(order);
            }
            orderExpiryService.cancelPendingPayments(order);
        }

        order.setStatus(next);
        order.setUpdatedAt(LocalDateTime.now());
        if (request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()) {
            order.setTrackingNumber(request.getTrackingNumber().trim());
        }
        orderRepository.save(order);
        recordHistory(order, next);

        // Customer-facing Telegram DM — no-ops internally for statuses with no customer
        // message (e.g. NEW) and for users without a linked telegram_id.
        telegramNotificationService.notifyCustomerOrderStatus(order);

        if (next == OrderStatus.SHIPPED) {
            if (order.getAppUser() != null) {
                emailService.sendOrderShippedEmail(order.getAppUser().getEmail(), order, null);
            }
            telegramNotificationService.notifyOrderShipped(order);
        } else if (next == OrderStatus.DELIVERED) {
            if (order.getAppUser() != null) {
                emailService.sendOrderDeliveredEmail(order.getAppUser().getEmail(), order);
            }
            telegramNotificationService.notifyOrderDelivered(order);
        }

        Order reloaded = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заказ не найден после обновления: " + id));
        return orderMapper.toResponse(reloaded);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userEmail) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        return orderRepository.findByAppUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(o -> {
                    OrderResponse r = orderMapper.toResponse(o);
                    r.setCdekShipment(cdekShipmentService.getByOrder(o.getId()));
                    return r;
                })
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Order history helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Appends an entry to the order_history audit log.
     * Called after every status assignment (creation → NEW, updates → next status).
     */
    private void recordHistory(Order order, OrderStatus status) {
        OrderHistory entry = new OrderHistory();
        entry.setOrder(order);
        entry.setStatus(status);
        entry.setDateAdded(new Date());
        orderHistoryRepository.save(entry);
    }

    /**
     * Explicit allowlist of valid admin-driven status transitions.
     * System transitions (PENDING_PAYMENT → CONFIRMED, → EXPIRED) are handled by payment/expiry
     * services directly and bypass this guard. Admin can only move orders forward or cancel them.
     * CANCELLED → CANCELLED is permitted as an idempotent no-op.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING_PAYMENT, Set.of(OrderStatus.CANCELLED),
            OrderStatus.NEW,             Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED,       Set.of(OrderStatus.IN_PRODUCTION, OrderStatus.CANCELLED),
            OrderStatus.IN_PRODUCTION,   Set.of(OrderStatus.READY, OrderStatus.CANCELLED),
            OrderStatus.READY,           Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED,         Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
            OrderStatus.DELIVERED,       Set.of(),
            OrderStatus.CANCELLED,       Set.of(OrderStatus.CANCELLED),
            OrderStatus.EXPIRED,         Set.of()
    );

    private static void assertAllowedStatusTransition(OrderStatus current, OrderStatus next) {
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new BusinessRuleException(
                    "Переход статуса «" + current + "» → «" + next + "» недопустим");
        }
    }

    private void validateCreateOrderRequest(CreateOrderRequest request) {
        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            throw new BusinessRuleException("Имя клиента обязательно");
        }

        if (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
            throw new BusinessRuleException("Телефон клиента обязателен");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessRuleException("Нельзя создать заказ без товаров");
        }

        if (request.getDeliveryType() == null) {
            throw new BusinessRuleException("Тип доставки обязателен");
        }

        // userAddressId and inline address are mutually exclusive
        if (request.getUserAddressId() != null && request.getAddress() != null) {
            throw new BusinessRuleException(
                    "Нельзя одновременно указывать userAddressId и address — используйте одно из двух");
        }
    }

    private static boolean requiresDeliveryAddress(DeliveryType type) {
        // Every method except in-store pickup needs a delivery address.
        return type != DeliveryType.PICKUP;
    }

    private static String normalizeIso2(String s) {
        return (s == null || s.isBlank()) ? null : s.trim().toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private void validateDeliveryAddress(DeliveryAddressRequest a) {
        if (isBlank(a.getCity())) {
            throw new BusinessRuleException("Укажите город");
        }
        if (isBlank(a.getStreet())) {
            throw new BusinessRuleException("Укажите улицу и дом");
        }
        if (isBlank(a.getApartment())) {
            throw new BusinessRuleException("Укажите квартиру / офис (или «—» если не требуется)");
        }
        if (isBlank(a.getPostalCode())) {
            throw new BusinessRuleException("Укажите почтовый индекс");
        }
        if (isBlank(a.getRecipientName())) {
            throw new BusinessRuleException("Укажите имя получателя");
        }
        if (isBlank(a.getRecipientPhone())) {
            throw new BusinessRuleException("Укажите телефон получателя");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Resolves the delivery address for this order.
     *
     * <ul>
     *   <li>If {@code userAddressId} is set: loads the saved {@link UserAddress}, verifies ownership,
     *       then copies its fields into a new {@link DeliveryAddressRequest}.
     *       No FK is created between the snapshot and the saved address — the snapshot is immutable.</li>
     *   <li>Otherwise: returns {@code request.getAddress()} (inline path, existing behaviour).</li>
     * </ul>
     */
    private DeliveryAddressRequest resolveDeliveryAddress(CreateOrderRequest request) {
        if (request.getUserAddressId() == null) {
            return request.getAddress();    // inline address — existing path, unchanged
        }

        UserAddress saved = userAddressRepository.findById(request.getUserAddressId())
                .orElseThrow(() -> new NotFoundException(
                        "Сохранённый адрес не найден: " + request.getUserAddressId()));

        // Ownership check — only the authenticated user's own address may be used
        AppUser currentUser = resolveAuthenticatedUser();
        if (currentUser == null || !saved.getAppUser().getId().equals(currentUser.getId())) {
            throw new BusinessRuleException("Нельзя использовать чужой адрес");
        }

        // Snapshot: copy fields — DeliveryAddress has no FK back to UserAddress
        return DeliveryAddressRequest.builder()
                .city(saved.getCity())
                .street(saved.getStreet())
                .apartment(saved.getApartment())
                .postalCode(saved.getPostalCode())
                .recipientName(saved.getRecipientName())
                .recipientPhone(saved.getRecipientPhone())
                .build();
    }

    /**
     * Returns the {@link AppUser} for the currently authenticated principal,
     * or {@code null} if the request is anonymous.
     */
    private AppUser resolveAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        if (!(auth.getPrincipal() instanceof UserDetails ud)) return null;
        return appUserRepository.findByEmailIgnoreCase(ud.getUsername()).orElse(null);
    }

    private Customer createCustomer(CreateOrderRequest request) {
        Customer customer = new Customer();
        customer.setName(request.getCustomerName());
        customer.setPhone(request.getCustomerPhone());
        customer.setTelegramUserName(request.getTelegramUsername());
        customer.setCreateAt(LocalDate.now());
        return customerRepository.save(customer);
    }

    private static void validateProductVariant(Product product, OrderItemRequest item) {
        List<String> sizes = product.getSizes();
        List<ProductColorOption> colors = product.getColors();
        boolean hasSizes = sizes != null && !sizes.isEmpty();
        boolean hasColors = colors != null && !colors.isEmpty();

        if (hasSizes) {
            if (item.getSize() == null || item.getSize().isBlank()) {
                throw new BusinessRuleException("Укажите размер для товара «" + product.getTitle() + "»");
            }
            String s = item.getSize().trim();
            if (!sizes.contains(s)) {
                throw new BusinessRuleException("Недопустимый размер для «" + product.getTitle() + "»");
            }
        }

        if (hasColors) {
            if (item.getColor() == null || item.getColor().isBlank()) {
                throw new BusinessRuleException("Укажите цвет для товара «" + product.getTitle() + "»");
            }
            String c = item.getColor().trim();
            boolean ok = colors.stream()
                    .filter(Objects::nonNull)
                    .map(ProductColorOption::getName)
                    .filter(Objects::nonNull)
                    .anyMatch(name -> name.trim().equalsIgnoreCase(c));
            if (!ok) {
                throw new BusinessRuleException("Недопустимый цвет для «" + product.getTitle() + "»");
            }
        }
    }

    private static String resolveSizeLabel(Product product, OrderItemRequest item) {
        if (product.getSizes() == null || product.getSizes().isEmpty()) {
            return null;
        }
        return item.getSize() != null ? item.getSize().trim() : null;
    }

    private static String resolveColorName(Product product, OrderItemRequest item) {
        if (product.getColors() == null || product.getColors().isEmpty()) {
            return null;
        }
        if (item.getColor() == null || item.getColor().isBlank()) {
            return null;
        }
        String c = item.getColor().trim();
        return product.getColors().stream()
                .filter(Objects::nonNull)
                .map(ProductColorOption::getName)
                .filter(Objects::nonNull)
                .filter(name -> name.trim().equalsIgnoreCase(c))
                .findFirst()
                .map(String::trim)
                .orElse(c);
    }
}
