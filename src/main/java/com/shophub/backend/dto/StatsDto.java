package com.shophub.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Numbers for the admin analytics dashboard. */
public record StatsDto(
        BigDecimal revenue,
        long orderCount,
        BigDecimal averageOrderValue,
        long customerCount,
        long productCount,
        long lowStockCount,
        long pendingReturns,
        Map<String, Long> ordersByStatus,
        List<DayStat> revenueByDay,
        List<TopProduct> topProducts
) {
    public record DayStat(String date, BigDecimal revenue, long orders) {}
    public record TopProduct(Long productId, String name, long units, BigDecimal revenue) {}
}
