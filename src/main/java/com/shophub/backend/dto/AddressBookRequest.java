package com.shophub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressBookRequest(
        @Size(max = 40) String label,
        @NotBlank(message = "First name is required") @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        @Pattern(regexp = "^$|^[0-9+()\\- ]{7,20}$", message = "Phone number is not valid") String phone,
        @NotBlank(message = "Address is required") @Size(max = 300) String address,
        @NotBlank(message = "City is required") @Size(max = 100) String city,
        @NotBlank(message = "PIN / postal code is required") @Size(max = 12) String zipCode,
        @NotBlank(message = "Country is required") @Size(max = 60) String country,
        Boolean makeDefault
) {}
