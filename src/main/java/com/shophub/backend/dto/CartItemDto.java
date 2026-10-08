package com.shophub.backend.dto;

import java.math.BigDecimal;

/**
 * Same shape as the frontend `CartItem` (Product fields + quantity).
 * `id` is the PRODUCT id. Also used for the line items of an order.
 */
public record CartItemDto(
        Long id,
        String name,
        BigDecimal price,
        String image,
        String category,
        Double rating,
        String description,
        Integer stock,
        int quantity
) {}
