package com.shophub.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than 0") BigDecimal price,
        String image,
        @NotBlank String category,
        Double rating,
        String description,
        @Min(0) @Max(1_000_000) Integer stock
) {}
