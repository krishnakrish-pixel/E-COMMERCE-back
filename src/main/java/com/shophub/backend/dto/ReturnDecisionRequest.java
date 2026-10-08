package com.shophub.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** decision = "approve" or "reject" */
public record ReturnDecisionRequest(@NotBlank String decision) {}
