package com.shophub.backend.dto;

import jakarta.validation.constraints.Max;

public record QuantityRequest(@Max(100) int quantity) {}
