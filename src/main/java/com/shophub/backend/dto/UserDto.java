package com.shophub.backend.dto;

/** Shape matches the frontend `User` interface (id is a string). Never contains the password. */
public record UserDto(
        String id,
        String email,
        String firstName,
        String lastName,
        String role,
        String phone,
        String address,
        String city,
        String zipCode,
        String country
) {}
