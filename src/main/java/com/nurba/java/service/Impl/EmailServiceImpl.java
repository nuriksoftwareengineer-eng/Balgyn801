package com.nurba.java.service.Impl;

import com.nurba.java.domain.Order;
import com.nurba.java.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${spring.mail.from:noreply@balgyn.kz}")
    private String from;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    @Async
    public void sendRegistrationEmail(String to, String name) {
        if (!enabled) return;
        String html = buildRegistrationHtml(name);
        send(to, "Добро пожаловать в BALGYN!", html);
    }

    @Override
    @Async
    public void sendOrderCreatedEmail(String to, Order order) {
        if (!enabled) return;
        String html = buildOrderHtml("Заказ оформлен", order,
                "Ваш заказ #" + order.getId() + " успешно создан. Ожидайте подтверждения после оплаты.");
        send(to, "Заказ #" + order.getId() + " оформлен – BALGYN", html);
    }

    @Override
    @Async
    public void sendPaymentSuccessEmail(String to, Order order) {
        if (!enabled) return;
        String html = buildOrderHtml("Оплата прошла", order,
                "Оплата заказа #" + order.getId() + " успешно получена. Ваш заказ передан в производство.");
        send(to, "Оплата подтверждена – BALGYN", html);
    }

    @Override
    @Async
    public void sendPaymentFailedEmail(String to, Order order) {
        if (!enabled) return;
        String html = buildOrderHtml("Ошибка оплаты", order,
                "К сожалению, оплата заказа #" + order.getId() + " не прошла. Попробуйте снова.");
        send(to, "Ошибка оплаты – BALGYN", html);
    }

    @Override
    @Async
    public void sendOrderShippedEmail(String to, Order order, String trackingNumber) {
        if (!enabled) return;
        String extra = trackingNumber != null ? "Трек-номер: <b>" + esc(trackingNumber) + "</b><br/>" : "";
        String html = buildOrderHtml("Заказ отправлен", order,
                "Ваш заказ #" + order.getId() + " отправлен!<br/>" + extra);
        send(to, "Заказ #" + order.getId() + " отправлен – BALGYN", html);
    }

    @Override
    @Async
    public void sendOrderDeliveredEmail(String to, Order order) {
        if (!enabled) return;
        String html = buildOrderHtml("Заказ доставлен", order,
                "Ваш заказ #" + order.getId() + " доставлен. Спасибо, что выбрали BALGYN!");
        send(to, "Заказ #" + order.getId() + " доставлен – BALGYN", html);
    }

    private void send(String to, String subject, String html) {
        if (mailSender == null) { log.debug("JavaMailSender not configured — skipping email to {}", to); return; }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildRegistrationHtml(String name) {
        return baseHtml("Добро пожаловать!",
                "<p>Здравствуйте, " + esc(name) + "!</p>" +
                "<p>Добро пожаловать в <b>BALGYN</b> — магазин эксклюзивной вышивки.</p>" +
                "<p>Теперь вы можете сохранять избранное, отслеживать заказы и управлять доставкой.</p>" +
                "<a href='" + frontendUrl + "' style='display:inline-block;margin-top:16px;padding:12px 28px;background:#18181b;color:#fff;border-radius:6px;text-decoration:none;font-weight:600'>Перейти в магазин</a>"
        );
    }

    private String buildOrderHtml(String title, Order order, String message) {
        String items = buildItemsTable(order);
        return baseHtml(title,
                "<p>" + message + "</p>" +
                items +
                "<p style='margin-top:16px'>Сумма заказа: <b>" + order.getTotalPrice() + " ₸</b></p>" +
                "<a href='" + frontendUrl + "/orders' style='display:inline-block;margin-top:16px;padding:12px 28px;background:#18181b;color:#fff;border-radius:6px;text-decoration:none;font-weight:600'>Мои заказы</a>"
        );
    }

    private String buildItemsTable(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) return "";
        StringBuilder sb = new StringBuilder("<table style='width:100%;border-collapse:collapse;margin-top:12px'>");
        sb.append("<tr style='background:#f4f4f5'><th style='padding:8px;text-align:left'>Позиция</th><th style='padding:8px;text-align:right'>Кол-во</th><th style='padding:8px;text-align:right'>Цена</th></tr>");
        order.getOrderItems().forEach(item -> sb
                .append("<tr><td style='padding:8px;border-top:1px solid #e4e4e7'>")
                .append("Изделие")
                .append("</td><td style='padding:8px;border-top:1px solid #e4e4e7;text-align:right'>")
                .append(item.getQuantity() != null ? item.getQuantity() : 1)
                .append("</td><td style='padding:8px;border-top:1px solid #e4e4e7;text-align:right'>")
                .append(item.getUnitPrice() != null ? item.getUnitPrice() : "—").append(" ₸")
                .append("</td></tr>")
        );
        sb.append("</table>");
        return sb.toString();
    }

    private String baseHtml(String title, String body) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f4f4f5;font-family:sans-serif'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:32px 16px'>" +
               "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:12px;overflow:hidden'>" +
               "<tr><td style='background:#18181b;padding:24px 32px'>" +
               "<span style='color:#fff;font-size:24px;font-weight:700;letter-spacing:2px'>BALGYN</span>" +
               "</td></tr>" +
               "<tr><td style='padding:32px'>" +
               "<h2 style='margin:0 0 16px;color:#18181b'>" + title + "</h2>" +
               body +
               "</td></tr>" +
               "<tr><td style='padding:16px 32px;background:#f4f4f5;color:#71717a;font-size:12px;text-align:center'>" +
               "© 2025 BALGYN. Все права защищены." +
               "</td></tr></table></td></tr></table></body></html>";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
