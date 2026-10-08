package com.shophub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Shipping address. The server checks it too, not only the browser. */
public record AddressDto(
        @NotBlank(message = "First name is required") @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        @NotBlank(message = "Address is required") @Size(max = 300) String address,
        @NotBlank(message = "City is required") @Size(max = 100) String city,
        @NotBlank(message = "PIN / postal code is required") @Size(max = 12) String zipCode,
        @NotBlank(message = "Country is required") @Size(max = 60) String country
) {}
