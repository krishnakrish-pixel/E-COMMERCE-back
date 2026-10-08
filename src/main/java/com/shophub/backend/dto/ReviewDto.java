package com.shophub.backend.dto;

import java.time.Instant;

public record ReviewDto(
        String id,
        Long productId,
        String userId,
        String userName,
        int rating,
        String comment,
        Instant createdAt
) {}
