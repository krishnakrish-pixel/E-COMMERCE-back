package com.shophub.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record StatusRequest(@NotBlank String status) {}
