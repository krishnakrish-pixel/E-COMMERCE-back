package com.shophub.backend.controller;

import com.shophub.backend.dto.ReviewDto;
import com.shophub.backend.dto.ReviewRequest;
import com.shophub.backend.entity.User;
import com.shophub.backend.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** Public: reviews shown on the product page. */
    @GetMapping("/products/{productId}/reviews")
    public List<ReviewDto> forProduct(@PathVariable Long productId) {
        return reviewService.forProduct(productId);
    }

    /** Verified buyers only. Posting again replaces the customer's earlier review. */
    @PostMapping("/reviews")
    public ResponseEntity<ReviewDto> save(@AuthenticationPrincipal User user, @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.save(user, request));
    }

    /** Owner or admin. */
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        reviewService.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
