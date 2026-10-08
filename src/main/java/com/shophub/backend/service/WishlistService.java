package com.shophub.backend.service;

import com.shophub.backend.entity.Product;
import com.shophub.backend.entity.User;
import com.shophub.backend.entity.WishlistItem;
import com.shophub.backend.exception.ApiException;
import com.shophub.backend.repository.ProductRepository;
import com.shophub.backend.repository.WishlistItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final ProductRepository productRepository;

    public WishlistService(WishlistItemRepository wishlistRepository, ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> list(User user) {
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(WishlistItem::getProduct).toList();
    }

    @Transactional
    public List<Product> add(User user, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        if (wishlistRepository.findByUserIdAndProductId(user.getId(), productId).isEmpty()) {
            WishlistItem item = new WishlistItem();
            item.setUser(user);
            item.setProduct(product);
            item.setCreatedAt(Instant.now());
            wishlistRepository.save(item);
        }
        return list(user);
    }

    @Transactional
    public List<Product> remove(User user, Long productId) {
        wishlistRepository.findByUserIdAndProductId(user.getId(), productId).ifPresent(wishlistRepository::delete);
        return list(user);
    }
}
