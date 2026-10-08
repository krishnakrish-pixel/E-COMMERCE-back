package com.shophub.backend.dto;

import java.math.BigDecimal;

/** What the checkout page shows after a code is applied. */
public record CouponResult(String code, String description, String discountType,
                           BigDecimal discountValue, BigDecimal discount) {}
