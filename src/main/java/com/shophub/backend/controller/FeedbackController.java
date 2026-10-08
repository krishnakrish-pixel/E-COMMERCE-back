package com.shophub.backend.controller;

import com.shophub.backend.entity.Feedback;
import com.shophub.backend.entity.User;
import com.shophub.backend.repository.FeedbackRepository;
import com.shophub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> submitFeedback(Authentication auth, @RequestBody Map<String, Object> payload) {
        User user = userRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "User not found"));
        }

        Long orderId = Long.valueOf(payload.get("orderId").toString());
        Integer rating = Integer.valueOf(payload.get("rating").toString());
        String comments = payload.get("comments") != null ? payload.get("comments").toString() : "";

        Feedback feedback = new Feedback();
        feedback.setUserId(user.getId());
        feedback.setOrderId(orderId);
        feedback.setRating(rating);
        feedback.setComments(comments);
        feedback.setCreatedAt(Instant.now());

        feedbackRepository.save(feedback);

        return ResponseEntity.ok(Map.of("message", "Feedback submitted successfully", "id", feedback.getId()));
    }

    @GetMapping
    public ResponseEntity<List<Feedback>> getAllFeedback() {
        return ResponseEntity.ok(feedbackRepository.findAll());
    }
}

