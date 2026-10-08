package com.shophub.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Sent right after login to push the guest (localStorage) cart into the user's saved cart. */
public record CartMergeRequest(@NotNull @Valid List<CartItemRequest> items) {}
