package com.nurba.java.service.Impl;

import com.nurba.java.dto.responce.DashboardStatsResponse;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.service.DashboardService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    @PersistenceContext
    private EntityManager em;

    private static final List<OrderStatus> PAID_STATUSES = List.of(
            OrderStatus.CONFIRMED, OrderStatus.IN_PRODUCTION,
            OrderStatus.READY, OrderStatus.SHIPPED, OrderStatus.DELIVERED
    );

    @Override
    public DashboardStatsResponse getStats(int days) {
        LocalDateTime since = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // KPIs
        long totalOrders = count("SELECT COUNT(o) FROM Order o WHERE o.status IN :s", PAID_STATUSES);
        long ordersToday = countSince("SELECT COUNT(o) FROM Order o WHERE o.status IN :s AND o.createdAt >= :d", PAID_STATUSES, todayStart);
        BigDecimal revenueTotal = sum("SELECT COALESCE(SUM(o.totalPrice),0) FROM Order o WHERE o.status IN :s", PAID_STATUSES);
        BigDecimal revenueToday = sumSince("SELECT COALESCE(SUM(o.totalPrice),0) FROM Order o WHERE o.status IN :s AND o.createdAt >= :d", PAID_STATUSES, todayStart);
        long totalUsers = (Long) em.createQuery("SELECT COUNT(u) FROM AppUser u").getSingleResult();
        long usersToday = (Long) em.createQuery("SELECT COUNT(u) FROM AppUser u WHERE u.createdAt >= :d")
                .setParameter("d", todayStart.toInstant(java.time.ZoneOffset.UTC)).getSingleResult();
        BigDecimal avgOrderValue = totalOrders == 0 ? BigDecimal.ZERO :
                revenueTotal.divide(BigDecimal.valueOf(totalOrders), 2, java.math.RoundingMode.HALF_UP);

        // Daily stats
        List<DashboardStatsResponse.DailyStat> dailyRevenue = buildDailyStats(since, days);

        // Top designs by order count. OrderItem has no "name"/"price" of its own —
        // the display name comes from either the linked design (catalog orders) or
        // product (legacy orders), so both are joined and coalesced.
        @SuppressWarnings("unchecked")
        List<Object[]> topRows = em.createQuery(
                "SELECT COALESCE(d.name, p.title), COUNT(oi), COALESCE(SUM(oi.unitPrice * oi.quantity),0) " +
                "FROM OrderItem oi " +
                "LEFT JOIN oi.designGarment dg LEFT JOIN dg.design d " +
                "LEFT JOIN oi.product p " +
                "WHERE oi.order.status IN :s " +
                "GROUP BY COALESCE(d.name, p.title) ORDER BY COUNT(oi) DESC")
                .setParameter("s", PAID_STATUSES)
                .setMaxResults(10)
                .getResultList();

        List<DashboardStatsResponse.DesignStat> topDesigns = topRows.stream()
                .map(r -> new DashboardStatsResponse.DesignStat(
                        (String) r[0], (Long) r[1], (BigDecimal) r[2]))
                .toList();

        // Orders by status
        @SuppressWarnings("unchecked")
        List<Object[]> statusRows = em.createQuery(
                "SELECT o.status, COUNT(o) FROM Order o WHERE o.status NOT IN :excl GROUP BY o.status")
                .setParameter("excl", List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.EXPIRED))
                .getResultList();

        List<DashboardStatsResponse.StatusStat> ordersByStatus = statusRows.stream()
                .map(r -> new DashboardStatsResponse.StatusStat(r[0].toString(), (Long) r[1]))
                .toList();

        return new DashboardStatsResponse(
                totalOrders, ordersToday, revenueTotal, revenueToday,
                totalUsers, usersToday, avgOrderValue,
                dailyRevenue, topDesigns, ordersByStatus
        );
    }

    private List<DashboardStatsResponse.DailyStat> buildDailyStats(LocalDateTime since, int days) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                "SELECT CAST(o.createdAt AS date), COUNT(o), COALESCE(SUM(o.totalPrice),0) " +
                "FROM Order o WHERE o.status IN :s AND o.createdAt >= :since " +
                "GROUP BY CAST(o.createdAt AS date) ORDER BY CAST(o.createdAt AS date)")
                .setParameter("s", PAID_STATUSES)
                .setParameter("since", since)
                .getResultList();

        Map<String, Object[]> byDate = rows.stream()
                .collect(Collectors.toMap(r -> r[0].toString(), r -> r));

        List<DashboardStatsResponse.DailyStat> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = days - 1; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(fmt);
            Object[] row = byDate.get(date);
            result.add(new DashboardStatsResponse.DailyStat(
                    date,
                    row != null ? (BigDecimal) row[2] : BigDecimal.ZERO,
                    row != null ? (Long) row[1] : 0L
            ));
        }
        return result;
    }

    private long count(String jpql, List<OrderStatus> statuses) {
        return (Long) em.createQuery(jpql).setParameter("s", statuses).getSingleResult();
    }

    private long countSince(String jpql, List<OrderStatus> statuses, LocalDateTime since) {
        return (Long) em.createQuery(jpql).setParameter("s", statuses).setParameter("d", since).getSingleResult();
    }

    private BigDecimal sum(String jpql, List<OrderStatus> statuses) {
        return (BigDecimal) em.createQuery(jpql).setParameter("s", statuses).getSingleResult();
    }

    private BigDecimal sumSince(String jpql, List<OrderStatus> statuses, LocalDateTime since) {
        return (BigDecimal) em.createQuery(jpql).setParameter("s", statuses).setParameter("d", since).getSingleResult();
    }
}
