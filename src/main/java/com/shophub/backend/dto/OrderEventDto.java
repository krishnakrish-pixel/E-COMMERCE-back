package com.shophub.backend.dto;

import java.time.Instant;

public record OrderEventDto(String status, String note, Instant createdAt) {}
