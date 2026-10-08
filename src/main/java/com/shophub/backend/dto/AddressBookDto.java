package com.shophub.backend.dto;

public record AddressBookDto(
        String id,
        String label,
        String firstName,
        String lastName,
        String phone,
        String address,
        String city,
        String zipCode,
        String country,
        boolean defaultAddress
) {}
