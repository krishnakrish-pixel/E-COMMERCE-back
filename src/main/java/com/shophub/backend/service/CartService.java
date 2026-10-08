package com.shophub.backend.service;

import com.shophub.backend.dto.CartItemDto;
import com.shophub.backend.dto.CartItemRequest;
import com.shophub.backend.entity.Cart;
import com.shophub.backend.entity.CartItem;
import com.shophub.backend.entity.Product;
import com.shophub.backend.entity.User;
import com.shophub.backend.exception.ApiException;
import com.shophub.backend.repository.CartRepository;
import com.shophub.backend.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private static final int MAX_QTY = 100;

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public List<CartItemDto> getCart(User user) {
        return toDtos(getOrCreateCart(user));
    }

    @Transactional
    public List<CartItemDto> addItem(User user, Long productId, int quantity) {
        Cart cart = getOrCreateCart(user);
        addToCart(cart, productId, quantity, true);
        return toDtos(cartRepository.save(cart));
    }

    /** Merges a guest cart (from the browser) into the saved cart. */
    @Transactional
    public List<CartItemDto> merge(User user, List<CartItemRequest> items) {
        Cart cart = getOrCreateCart(user);
        for (CartItemRequest req : items) {
            // skip products that no longer exist instead of failing the whole login
            if (productRepository.existsById(req.productId())) {
                addToCart(cart, req.productId(), req.quantity(), false);
            }
        }
        return toDtos(cartRepository.save(cart));
    }

    @Transactional
    public List<CartItemDto> setQuantity(User user, Long productId, int quantity) {
        Cart cart = getOrCreateCart(user);
        if (quantity <= 0) {
            cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        } else {
            CartItem item = findItem(cart, productId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item is not in your cart"));
            int limit = limitFor(item.getProduct());
            if (quantity > limit) {
                throw new ApiException(HttpStatus.CONFLICT, limit <= 0
                        ? item.getProduct().getName() + " is out of stock"
                        : "Only " + limit + " left in stock for " + item.getProduct().getName());
            }
            item.setQuantity(quantity);
        }
        return toDtos(cartRepository.save(cart));
    }

    @Transactional
    public List<CartItemDto> removeItem(User user, Long productId) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        return toDtos(cartRepository.save(cart));
    }

    /** After an order: only the products that were bought leave the cart (the rest stays for later). */
    @Transactional
    public void removeProducts(User user, java.util.Set<Long> productIds) {
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            cart.getItems().removeIf(i -> productIds.contains(i.getProduct().getId()));
            cartRepository.save(cart);
        });
    }

    @Transactional
    public void clear(User user) {
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    // ---------------------------------------------------------------- helpers

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    /** Highest quantity of this product a cart may hold (stock, capped at 100). */
    private int limitFor(Product product) {
        Integer stock = product.getStock();
        return stock == null ? MAX_QTY : Math.max(0, Math.min(stock, MAX_QTY));
    }

    /**
     * @param strict true  = tell the customer (409) when there is not enough stock (normal "add to cart")
     *               false = quietly cap at the available stock (used when merging a guest cart after login)
     */
    private void addToCart(Cart cart, Long productId, int quantity, boolean strict) {
        int qty = Math.max(1, quantity);
        Optional<CartItem> existing = findItem(cart, productId);
        Product product = existing.isPresent()
                ? existing.get().getProduct()
                : productRepository.findById(productId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        int limit = limitFor(product);
        int wanted = (existing.isPresent() ? existing.get().getQuantity() : 0) + qty;

        if (wanted > limit) {
            if (strict) {
                throw new ApiException(HttpStatus.CONFLICT, limit <= 0
                        ? product.getName() + " is out of stock"
                        : "Only " + limit + " left in stock for " + product.getName());
            }
            wanted = limit;
            if (wanted <= 0) return;                 // sold out: skip it
        }

        if (existing.isPresent()) {
            existing.get().setQuantity(wanted);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(wanted);
            cart.getItems().add(item);
        }
    }

    private Optional<CartItem> findItem(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst();
    }

    private List<CartItemDto> toDtos(Cart cart) {
        return cart.getItems().stream()
                .map(i -> {
                    Product p = i.getProduct();
                    return new CartItemDto(p.getId(), p.getName(), p.getPrice(), p.getImage(),
                            p.getCategory(), p.getRating(), p.getDescription(), p.getStock(), i.getQuantity());
                })
                .toList();
    }
}
