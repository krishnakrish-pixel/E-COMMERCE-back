package com.shophub.backend.service;

import com.shophub.backend.dto.ReviewDto;
import com.shophub.backend.dto.ReviewRequest;
import com.shophub.backend.entity.Order;
import com.shophub.backend.entity.Product;
import com.shophub.backend.entity.Review;
import com.shophub.backend.entity.User;
import com.shophub.backend.exception.ApiException;
import com.shophub.backend.repository.OrderRepository;
import com.shophub.backend.repository.ProductRepository;
import com.shophub.backend.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         ProductRepository productRepository,
                         OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> forProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Product not found");
        }
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream().map(this::toDto).toList();
    }

    /** Creates the review, or replaces the customer's earlier review of the same product. */
    @Transactional
    public ReviewDto save(User user, ReviewRequest req) {
        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        if (!hasPurchased(user, product.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only customers who bought this product can review it");
        }

        Review review = reviewRepository.findByUserIdAndProductId(user.getId(), product.getId()).orElseGet(() -> {
            Review r = new Review();
            r.setUser(user);
            r.setProduct(product);
            return r;
        });
        review.setRating(req.rating());
        review.setComment(req.comment() == null ? "" : req.comment().trim());
        review.setCreatedAt(Instant.now());
        Review saved = reviewRepository.save(review);

        refreshRating(product);
        return toDto(saved);
    }

    @Transactional
    public void delete(User actor, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Review not found"));
        boolean admin = "admin".equalsIgnoreCase(actor.getRole());
        if (!admin && !review.getUser().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only delete your own review");
        }
        Product product = review.getProduct();
        reviewRepository.delete(review);
        refreshRating(product);
    }

    // ---------------------------------------------------------------- helpers

    /** "Verified purchase": the customer has a non-cancelled order containing the product. */
    private boolean hasPurchased(User user, Long productId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return orders.stream()
                .filter(o -> !"cancelled".equals(o.getStatus()))
                .anyMatch(o -> o.getItems().stream().anyMatch(i -> productId.equals(i.getProductId())));
    }

    /** Product rating = average of its reviews (1 decimal). With no reviews the starting rating is kept. */
    private void refreshRating(Product product) {
        List<Review> all = reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId());
        product.setReviewCount(all.size());
        if (!all.isEmpty()) {
            double avg = all.stream().mapToInt(Review::getRating).average().orElse(0);
            product.setRating(Math.round(avg * 10.0) / 10.0);
        }
        productRepository.save(product);
    }

    private ReviewDto toDto(Review r) {
        User u = r.getUser();
        String first = u.getFirstName() == null ? "" : u.getFirstName().trim();
        String last = u.getLastName() == null ? "" : u.getLastName().trim();
        String name = (first + (last.isEmpty() ? "" : " " + last.charAt(0) + ".")).trim();
        return new ReviewDto(String.valueOf(r.getId()), r.getProduct().getId(), String.valueOf(u.getId()),
                name.isEmpty() ? "Customer" : name, r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
