package com.nurba.java.service.Impl;

import com.nurba.java.domain.AppUser;
import com.nurba.java.domain.DeliveryAddress;
import com.nurba.java.domain.Order;
import com.nurba.java.enums.DeliveryType;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.security.TelegramApiErrors;
import com.nurba.java.service.TelegramNotificationService;
import jakarta.annotation.PostConstruct;
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

    // Dedicated admin-alert bot. Blank = admin alerts fall back to the Mini App bot/chat above.
    // Customer-facing messages always go through the Mini App bot — customers only have a chat
    // relationship with the bot they logged in through.
    @Value("${app.telegram.admin-bot-token:}")
    private String adminBotToken;

    @Value("${app.telegram.admin-chat-id:}")
    private String adminChatId;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    private final RestClient restClient = RestClient.create();
    private final OrderRepository orderRepository;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public TelegramNotificationServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /** Startup visibility only — logs which bot configuration is active, never token values. */
    @PostConstruct
    void logStartupStatus() {
        if (!enabled) {
            log.info("[Telegram] Notifications disabled (app.telegram.enabled=false)");
            return;
        }
        boolean dedicatedAdminBot = adminBotToken != null && !adminBotToken.isBlank();
        String adminChat = effectiveAdminChatId();
        log.info("[Telegram] Notifications enabled: mini-app bot token {}; admin alerts via {} bot, admin chat id {}",
                (botToken != null && !botToken.isBlank()) ? "present" : "MISSING",
                dedicatedAdminBot ? "dedicated admin" : "mini-app (fallback)",
                (adminChat == null || adminChat.isBlank()) ? "MISSING" : "present");
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
    public void notifyError(String context, String message) {
        if (!enabled) return;
        send("⚠️ <b>Ошибка [" + context + "]</b>\n" + message);
    }

    // ─── Customer-facing events ──────────────────────────────────────────────────

    @Override
    @Async
    public void notifyCustomerOrderStatus(Order order) {
        if (!enabled) return;
        AppUser user = order.getAppUser();
        if (user == null || user.getTelegramId() == null) return;

        String text = customerStatusMessage(order);
        if (text == null) return;

        // Always the Mini App bot: the customer started (and trusts) that bot, not the admin one.
        sendMessage(botToken, String.valueOf(user.getTelegramId()), text);
    }

    private static String customerStatusMessage(Order order) {
        String orderRef = "Заказ #" + order.getId();
        return switch (order.getStatus()) {
            case CONFIRMED -> "✅ <b>" + orderRef + " принят</b>\nМы начинаем работу над вашим заказом.";
            case IN_PRODUCTION -> "🧵 <b>" + orderRef + " в производстве</b>\nВышиваем и шьём ваш заказ.";
            case READY -> "📦 <b>" + orderRef + " готов к отправке</b>";
            case SHIPPED -> {
                String tracking = order.getTrackingNumber();
                yield "🚚 <b>" + orderRef + " отправлен</b>"
                        + (tracking != null && !tracking.isBlank() ? "\nТрек-номер: " + tracking : "");
            }
            case DELIVERED -> "🎉 <b>" + orderRef + " доставлен</b>\nСпасибо за покупку!";
            case CANCELLED -> "❌ <b>" + orderRef + " отменён</b>";
            default -> null;
        };
    }

    // ─── Internal ────────────────────────────────────────────────────────────────

    /** Admin-facing alerts → admin bot; falls back to the Mini App bot when no dedicated one is set. */
    private void send(String text) {
        String targetChatId = effectiveAdminChatId();
        if (targetChatId == null || targetChatId.isBlank()) {
            log.info("[Telegram] Admin notification skipped — no admin chat id configured");
            return;
        }
        if (sendMessage(effectiveAdminBotToken(), targetChatId, text)) {
            log.info("[Telegram] Admin notification delivered to chat {}", targetChatId);
        }
    }

    private boolean sendMessage(String token, String targetChatId, String text) {
        if (token == null || token.isBlank() || targetChatId == null || targetChatId.isBlank()) return false;
        try {
            String url = "https://api.telegram.org/bot" + token + "/sendMessage";
            restClient.post()
                    .uri(url)
                    .body(Map.of("chat_id", targetChatId, "text", text, "parse_mode", "HTML"))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            // Not e.getMessage(): transport exceptions embed the token-bearing request URL.
            log.warn("Telegram notification failed: {}", TelegramApiErrors.describe(e));
            return false;
        }
    }

    // Fallback lives here (not in property placeholders) deliberately: docker compose's
    // `${VAR:-}` forwarding defines the variable as an EMPTY string in the container, which
    // Spring treats as "set" — a properties-level `${TELEGRAM_ADMIN_BOT_TOKEN:${TELEGRAM_BOT_TOKEN:}}`
    // default would therefore never fire under compose. A blank-check catches both unset and empty.
    private String effectiveAdminBotToken() {
        return adminBotToken != null && !adminBotToken.isBlank() ? adminBotToken : botToken;
    }

    private String effectiveAdminChatId() {
        return adminChatId != null && !adminChatId.isBlank() ? adminChatId : chatId;
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
