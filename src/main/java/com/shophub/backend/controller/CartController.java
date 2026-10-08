package com.shophub.backend.controller;

import com.shophub.backend.dto.CartItemDto;
import com.shophub.backend.dto.CartItemRequest;
import com.shophub.backend.dto.CartMergeRequest;
import com.shophub.backend.dto.QuantityRequest;
import com.shophub.backend.entity.User;
import com.shophub.backend.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Saved cart of the logged-in user. Every endpoint returns the full, updated cart. */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public List<CartItemDto> getCart(@AuthenticationPrincipal User user) {
        return cartService.getCart(user);
    }

    @PostMapping("/items")
    public List<CartItemDto> addItem(@AuthenticationPrincipal User user,
                                     @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(user, request.productId(), request.quantity());
    }

    @PostMapping("/merge")
    public List<CartItemDto> merge(@AuthenticationPrincipal User user,
                                   @Valid @RequestBody CartMergeRequest request) {
        return cartService.merge(user, request.items());
    }

    @PutMapping("/items/{productId}")
    public List<CartItemDto> setQuantity(@AuthenticationPrincipal User user,
                                         @PathVariable Long productId,
                                         @Valid @RequestBody QuantityRequest request) {
        return cartService.setQuantity(user, productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public List<CartItemDto> removeItem(@AuthenticationPrincipal User user, @PathVariable Long productId) {
        return cartService.removeItem(user, productId);
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@AuthenticationPrincipal User user) {
        cartService.clear(user);
        return ResponseEntity.noContent().build();
    }
}
