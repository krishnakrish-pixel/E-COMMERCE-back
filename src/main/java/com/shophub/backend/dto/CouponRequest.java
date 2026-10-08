package com.shophub.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponRequest(
        @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "may only contain letters, numbers, - and _") String code,
        @Size(max = 255) String description,
        @NotBlank @Pattern(regexp = "^(?i)(PERCENT|FIXED)$", message = "must be PERCENT or FIXED") String discountType,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than 0") BigDecimal discountValue,
        @DecimalMin("0.0") BigDecimal minOrderAmount,
        Integer maxUses,
        Instant expiresAt,
        Boolean active
) {}
