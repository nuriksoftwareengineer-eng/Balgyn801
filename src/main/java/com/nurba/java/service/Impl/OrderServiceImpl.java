package com.nurba.java.service.Impl;

import com.nurba.java.domain.Customer;
import com.nurba.java.domain.DeliveryAddress;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.OrderItem;
import com.nurba.java.domain.Product;
import com.nurba.java.dto.delivery.CdekOrderItemRequest;
import com.nurba.java.dto.delivery.CdekOrderTariffRequest;
import com.nurba.java.model.ProductColorOption;
import com.nurba.java.dto.request.CreateOrderRequest;
import com.nurba.java.dto.request.DeliveryAddressRequest;
import com.nurba.java.dto.request.OrderItemRequest;
import com.nurba.java.dto.request.UpdateOrderStatusRequest;
import com.nurba.java.dto.responce.OrderResponse;
import com.nurba.java.enums.DeliveryType;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.DeliveryMapper;
import com.nurba.java.mapper.OrderMapper;
import com.nurba.java.repositories.*;
import com.nurba.java.service.OrderService;
import com.nurba.java.service.delivery.CdekDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
    private final CdekDeliveryService cdekDeliveryService;


    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        validateCreateOrderRequest(request);

        Customer customer = createCustomer(request);

        Order order = new Order();
        order.setCustomer(customer);
        order.setDeliveryType(request.getDeliveryType());
        order.setStatus(OrderStatus.NEW);
        order.setComment(request.getComment());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setTotalPrice(BigDecimal.ZERO);

        Order savedOrder = orderRepository.save(order);

        BigDecimal itemsTotal = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new NotFoundException("Товар не найден"));

            if (Boolean.FALSE.equals(product.getInStock())) {
                throw new BusinessRuleException("Товар нет в наличии");
            }

            int quantity = itemRequest.getQuantity();

            if (quantity <= 0) {
                throw new BusinessRuleException("Количество должно быть больше 0");
            }

            validateProductVariant(product, itemRequest);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setSizeLabel(resolveSizeLabel(product, itemRequest));
            orderItem.setColorName(resolveColorName(product, itemRequest));

            orderItemRepository.save(orderItem);

            BigDecimal itemTotalPrice = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            itemsTotal = itemsTotal.add(itemTotalPrice);
        }

        BigDecimal deliveryFee = request.getDeliveryFee() != null
                ? request.getDeliveryFee().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        if (deliveryFee.signum() < 0) {
            throw new BusinessRuleException("Стоимость доставки не может быть отрицательной");
        }
        if (request.getDeliveryType() == DeliveryType.CDEK && deliveryFee.signum() <= 0) {
            throw new BusinessRuleException("Для доставки СДЭК сначала рассчитайте стоимость на сайте");
        }
        if (request.getDeliveryType() == DeliveryType.CDEK) {
            assertExpectedCdekFee(request, deliveryFee);
        }

        BigDecimal grandTotal = itemsTotal.add(deliveryFee);
        savedOrder.setDeliveryFee(deliveryFee.signum() > 0 ? deliveryFee : null);
        savedOrder.setTotalPrice(grandTotal);
        savedOrder.setUpdatedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(savedOrder);

        if (requiresDeliveryAddress(request.getDeliveryType())) {
            DeliveryAddressRequest addrReq = request.getAddress();
            if (addrReq == null) {
                throw new BusinessRuleException("Укажите адрес доставки для выбранного способа получения");
            }
            validateDeliveryAddress(addrReq);
            DeliveryAddress address = deliveryMapper.toEntity(addrReq);
            address.setOrder(updatedOrder);
            deliveryAddressRepository.save(address);
        }

        Order withRelations = orderRepository.findById(updatedOrder.getId())
                .orElseThrow(() -> new NotFoundException("Заказ не найден после создания"));
        return orderMapper.toResponse(withRelations);
    }

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
        return orderRepository.findAll()
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
        order.setStatus(next);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        Order reloaded = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заказ не найден после обновления: " + id));
        return orderMapper.toResponse(reloaded);
    }

    private static void assertAllowedStatusTransition(OrderStatus current, OrderStatus next) {
        if (current == OrderStatus.CANCELLED && next != OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Отменённый заказ нельзя вернуть в работу");
        }
        if (current == OrderStatus.DELIVERED && next != OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Доставленный заказ нельзя менять");
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

    }

    private static boolean requiresDeliveryAddress(DeliveryType type) {
        return type == DeliveryType.TAXI || type == DeliveryType.CDEK;
    }

    private void assertExpectedCdekFee(CreateOrderRequest request, BigDecimal deliveryFee) {
        DeliveryAddressRequest address = request.getAddress();
        if (address == null) {
            throw new BusinessRuleException("Для СДЭК укажите адрес ПВЗ");
        }
        validateDeliveryAddress(address);

        Integer toCityCode = resolveCdekCityCode(address.getCity());
        List<CdekOrderItemRequest> items = request.getItems().stream()
                .map(i -> new CdekOrderItemRequest(i.getProductId(), i.getQuantity()))
                .toList();
        BigDecimal expected = cdekDeliveryService.calculateOrder(
                        new CdekOrderTariffRequest(toCityCode, items, null))
                .deliveryPrice()
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal actual = deliveryFee.setScale(2, RoundingMode.HALF_UP);
        if (expected.compareTo(actual) != 0) {
            throw new BusinessRuleException(
                    "Стоимость доставки устарела или изменена. " +
                            "Пересчитайте СДЭК (ожидалось " + expected + ", передано " + actual + ")");
        }
    }

    private Integer resolveCdekCityCode(String rawCity) {
        if (isBlank(rawCity)) {
            throw new BusinessRuleException("Для СДЭК укажите город");
        }
        String cityName = rawCity.split(",")[0].trim();
        if (cityName.isBlank()) {
            throw new BusinessRuleException("Для СДЭК не удалось определить город");
        }
        var cities = cdekDeliveryService.searchCities(cityName, 10);
        return cities.stream()
                .filter(c -> c.city() != null)
                .filter(c -> c.city().trim().equalsIgnoreCase(cityName))
                .map(c -> c.code())
                .findFirst()
                .orElseGet(() -> cities.stream()
                        .filter(c -> c.city() != null)
                        .filter(c -> c.city().toLowerCase(Locale.ROOT)
                                .startsWith(cityName.toLowerCase(Locale.ROOT)))
                        .map(c -> c.code())
                        .findFirst()
                        .orElseThrow(() -> new BusinessRuleException(
                                "Город СДЭК не найден по адресу: " + cityName)));
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
