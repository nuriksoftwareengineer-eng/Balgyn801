package com.nurba.java.dto.responce;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStatsResponse(
        // KPI
        long totalOrders,
        long ordersToday,
        BigDecimal revenueTotal,
        BigDecimal revenueToday,
        long totalUsers,
        long usersToday,
        BigDecimal avgOrderValue,

        // Charts
        List<DailyStat> dailyRevenue,
        List<DesignStat> topDesigns,
        List<StatusStat> ordersByStatus
) {
    public record DailyStat(String date, BigDecimal revenue, long orders) {}
    public record DesignStat(String name, long count, BigDecimal revenue) {}
    public record StatusStat(String status, long count) {}
}
