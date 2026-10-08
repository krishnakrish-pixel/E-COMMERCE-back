package com.shophub.backend.service;

import com.shophub.backend.dto.ProductRequest;
import com.shophub.backend.entity.Product;
import com.shophub.backend.exception.ApiException;
import com.shophub.backend.repository.CartItemRepository;
import com.shophub.backend.repository.ProductRepository;
import com.shophub.backend.repository.ReviewRepository;
import com.shophub.backend.repository.WishlistItemRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    public static final int DEFAULT_STOCK = 50;

    private static final String DEFAULT_IMAGE =
            "https://images.unsplash.com/photo-1523275335684-37898b6baf30?q=80&w=600&auto=format&fit=crop";

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistItemRepository wishlistItemRepository;

    public ProductService(ProductRepository productRepository,
                          CartItemRepository cartItemRepository,
                          ReviewRepository reviewRepository,
                          WishlistItemRepository wishlistItemRepository) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.reviewRepository = reviewRepository;
        this.wishlistItemRepository = wishlistItemRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> getAll() {
        return productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @Transactional
    public Product create(ProductRequest req) {
        Product p = new Product();
        apply(p, req);
        if (p.getRating() == null) p.setRating(5.0);
        if (p.getStock() == null) p.setStock(DEFAULT_STOCK);
        return productRepository.save(p);
    }

    @Transactional
    public Product update(Long id, ProductRequest req) {
        Product p = getById(id);
        apply(p, req);
        return productRepository.save(p);
    }

    @Transactional
    public void delete(Long id) {
        Product p = getById(id);
        cartItemRepository.deleteByProductId(id);   // remove it from any carts first
        wishlistItemRepository.deleteByProductId(id);
        reviewRepository.deleteByProductId(id);
        productRepository.delete(p);                // past orders keep their own snapshot
    }

    private void apply(Product p, ProductRequest req) {
        p.setName(req.name().trim());
        p.setPrice(req.price());
        p.setCategory(req.category().trim());
        p.setImage(req.image() == null || req.image().isBlank() ? DEFAULT_IMAGE : req.image().trim());
        p.setDescription(req.description() == null || req.description().isBlank()
                ? "High-quality premium product." : req.description().trim());
        if (req.rating() != null) {
            p.setRating(Math.max(0.0, Math.min(5.0, req.rating())));
        }
        if (req.stock() != null) {
            p.setStock(req.stock());
        }
    }
}
