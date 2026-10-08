package com.shophub.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Only product ids + quantities are accepted. Prices and totals are always computed on the server. */
public record OrderRequest(
        @NotEmpty @Valid List<CartItemRequest> items,
        @NotNull @Valid AddressDto shippingAddress,
        String paymentMethod,
        String couponCode
) {}
