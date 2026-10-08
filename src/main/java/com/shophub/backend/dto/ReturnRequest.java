package com.shophub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReturnRequest(
        @NotBlank(message = "Please tell us why you want to return this order")
        @Size(min = 5, max = 500, message = "must be between 5 and 500 characters") String reason
) {}
