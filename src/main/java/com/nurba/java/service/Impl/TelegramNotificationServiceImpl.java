package com.nurba.java.service.Impl;

import com.nurba.java.domain.DeliveryAddress;
import com.nurba.java.domain.Order;
import com.nurba.java.enums.DeliveryType;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.service.TelegramNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class TelegramNotificationServiceImpl implements TelegramNotificationService {

    @Value("${app.telegram.enabled:false}")
    private boolean enabled;

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.chat-id:}")
    private String chatId;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    private final RestClient restClient = RestClient.create();
    private final OrderRepository orderRepository;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public TelegramNotificationServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // ─── New order (fires right after order creation, before payment) ──────────

    @Override
    @Async
    @Transactional(readOnly = true)
    public void notifyNewOrder(Order order) {
        if (!enabled) return;

        // Reload inside transaction so lazy collections are accessible
        Order o = orderRepository.findById(order.getId()).orElse(order);

        String customerName = o.getCustomer() != null ? o.getCustomer().getName() : "—";
        String phone = (o.getCustomer() != null && o.getCustomer().getPhone() != null
                && !o.getCustomer().getPhone().isBlank())
                ? o.getCustomer().getPhone() : null;
        String email = o.getAppUser() != null ? o.getAppUser().getEmail() : "—";
        int itemCount = o.getOrderItems().size();
        String delivery = deliveryLabel(o.getDeliveryType());
        String dateTime = o.getCreatedAt() != null ? o.getCreatedAt().format(DT_FMT) : "—";
        String adminLink = frontendBaseUrl + "/admin/orders/" + o.getId();

        StringBuilder sb = new StringBuilder();
        sb.append("🛍 <b>Новый заказ #").append(o.getId()).append("</b>\n\n");
        sb.append("👤 ").append(customerName).append("\n");
        sb.append("📧 ").append(email).append("\n");
        if (phone != null) sb.append("📱 ").append(phone).append("\n");
        sb.append("\n");
        sb.append("💰 <b>").append(formatPrice(o.getTotalPrice())).append(" ₸</b>")
                .append(" — ").append(itemCount).append(" ").append(pluralItems(itemCount)).append("\n");
        if (o.getDiscountAmount() != null && o.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("🏷 Скидка: -").append(formatPrice(o.getDiscountAmount())).append(" ₸");
            if (o.getCouponCode() != null) sb.append(" (").append(o.getCouponCode()).append(")");
            sb.append("\n");
        }
        sb.append("🚚 ").append(delivery).append("\n");
        DeliveryAddress addr = o.getDeliveryAddress();
        if (addr != null) {
            StringBuilder addrLine = new StringBuilder("📍 ");
            if (addr.getCity() != null && !addr.getCity().isBlank()) addrLine.append(addr.getCity());
            if (addr.getStreet() != null && !addr.getStreet().isBlank()) addrLine.append(", ").append(addr.getStreet());
            if (addr.getApartment() != null && !addr.getApartment().isBlank()) addrLine.append(", кв. ").append(addr.getApartment());
            if (addr.getPvzCode() != null && !addr.getPvzCode().isBlank()) addrLine.append(" (ПВЗ: ").append(addr.getPvzCode()).append(")");
            sb.append(addrLine).append("\n");
            if (addr.getRecipientName() != null && !addr.getRecipientName().isBlank()
                    && !addr.getRecipientName().equals(customerName)) {
                sb.append("👤 Получатель: ").append(addr.getRecipientName()).append("\n");
            }
            if (addr.getRecipientPhone() != null && !addr.getRecipientPhone().isBlank()) {
                sb.append("📱 ").append(addr.getRecipientPhone()).append("\n");
            }
        }
        sb.append("⏳ Ожидает оплаты\n");
        sb.append("📅 ").append(dateTime).append("\n\n");
        sb.append("<a href=\"").append(adminLink).append("\">Открыть в админке →</a>");

        send(sb.toString());
    }

    // Called after the creating transaction commits — guarantees order items are visible
    @Override
    @Async
    @Transactional(readOnly = true)
    public void notifyNewOrderById(Long orderId) {
        if (!enabled) return;
        Order o = orderRepository.findById(orderId).orElse(null);
        if (o == null) return;
        notifyNewOrder(o);
    }

    // ─── Payment events ─────────────────────────────────────────────────────────

    @Override
    @Async
    public void notifyPaymentSuccess(Order order) {
        if (!enabled) return;
        String text = "✅ <b>Оплата получена</b> — заказ #" + order.getId() + "\n" +
                "💰 " + formatPrice(order.getTotalPrice()) + " ₸\n" +
                "👤 " + (order.getCustomer() != null ? order.getCustomer().getName() : "—");
        send(text);
    }

    @Override
    @Async
    public void notifyPaymentFailed(Order order) {
        if (!enabled) return;
        String text = "❌ <b>Оплата не прошла</b> — заказ #" + order.getId() + "\n" +
                "💰 " + formatPrice(order.getTotalPrice()) + " ₸\n" +
                "👤 " + (order.getCustomer() != null ? order.getCustomer().getName() : "—");
        send(text);
    }

    // ─── Fulfillment events ──────────────────────────────────────────────────────

    @Override
    @Async
    public void notifyOrderShipped(Order order) {
        if (!enabled) return;
        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : "—";
        String adminLink = frontendBaseUrl + "/admin/orders/" + order.getId();
        String text = "📦 <b>Заказ #" + order.getId() + " отправлен</b>\n" +
                "👤 " + customerName + "\n" +
                "<a href=\"" + adminLink + "\">Открыть в админке →</a>";
        send(text);
    }

    @Override
    @Async
    public void notifyOrderDelivered(Order order) {
        if (!enabled) return;
        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : "—";
        String adminLink = frontendBaseUrl + "/admin/orders/" + order.getId();
        String text = "🎉 <b>Заказ #" + order.getId() + " доставлен</b>\n" +
                "👤 " + customerName + "\n" +
                "<a href=\"" + adminLink + "\">Открыть в админке →</a>";
        send(text);
    }

    // ─── System events ───────────────────────────────────────────────────────────

    @Override
    @Async
    public void notifyNewUser(String email) {
        if (!enabled) return;
        send("👤 <b>Новый пользователь:</b> " + email);
    }

    @Override
    @Async
    public void notifyError(String context, String message) {
        if (!enabled) return;
        send("⚠️ <b>Ошибка [" + context + "]</b>\n" + message);
    }

    // ─── Internal ────────────────────────────────────────────────────────────────

    private void send(String text) {
        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) return;
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            restClient.post()
                    .uri(url)
                    .body(Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Telegram notification failed: {}", e.getMessage());
        }
    }

    private static String deliveryLabel(DeliveryType type) {
        if (type == null) return "—";
        return switch (type) {
            case PICKUP -> "Самовывоз";
            case TAXI -> "Курьер (Яндекс)";
            case CDEK -> "СДЭК";
            case POSTAL -> "Почта Казахстана";
            case INTERNATIONAL -> "Международная доставка";
        };
    }

    private static String formatPrice(BigDecimal price) {
        if (price == null) return "0";
        long rounded = price.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        // Format with non-breaking space as thousands separator
        String raw = Long.toString(rounded);
        StringBuilder out = new StringBuilder();
        int start = raw.length() % 3;
        if (start > 0) out.append(raw, 0, start);
        for (int i = start; i < raw.length(); i += 3) {
            if (!out.isEmpty()) out.append(' '); // non-breaking space
            out.append(raw, i, i + 3);
        }
        return out.toString();
    }

    private static String pluralItems(int n) {
        int mod = n % 10;
        int mod100 = n % 100;
        if (mod == 1 && mod100 != 11) return "позиция";
        if (mod >= 2 && mod <= 4 && (mod100 < 10 || mod100 >= 20)) return "позиции";
        return "позиций";
    }
}
