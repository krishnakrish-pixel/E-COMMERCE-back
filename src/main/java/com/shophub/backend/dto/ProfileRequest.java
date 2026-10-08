package com.shophub.backend.dto;

public record ProfileRequest(
        String firstName,
        String lastName,
        String phone,
        String address,
        String city,
        String zipCode,
        String country
) {}
