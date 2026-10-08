package com.shophub.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponDto(
        String id,
        String code,
        String description,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        Integer maxUses,
        int usedCount,
        Instant expiresAt,
        boolean active
) {}
