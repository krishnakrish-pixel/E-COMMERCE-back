package com.shophub.backend.controller;

import com.shophub.backend.entity.Product;
import com.shophub.backend.entity.User;
import com.shophub.backend.service.WishlistService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Saved products of the logged-in user. Every endpoint returns the full, updated list. */
@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public List<Product> list(@AuthenticationPrincipal User user) {
        return wishlistService.list(user);
    }

    @PostMapping("/{productId}")
    public List<Product> add(@AuthenticationPrincipal User user, @PathVariable Long productId) {
        return wishlistService.add(user, productId);
    }

    @DeleteMapping("/{productId}")
    public List<Product> remove(@AuthenticationPrincipal User user, @PathVariable Long productId) {
        return wishlistService.remove(user, productId);
    }
}
