package com.shophub.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Shape matches the frontend `Order` interface. */
public record OrderDto(
        String id,
        String orderNumber,
        String userId,
        List<CartItemDto> items,
        BigDecimal subtotal,
        BigDecimal discount,
        String couponCode,
        BigDecimal shipping,
        BigDecimal tax,
        BigDecimal total,
        String status,
        String paymentMethod,
        AddressDto shippingAddress,
        Instant createdAt,
        Instant updatedAt,
        Instant estimatedDelivery,
        String trackingNumber,
        List<OrderEventDto> history,
        String returnStatus,
        String returnReason
) {}
