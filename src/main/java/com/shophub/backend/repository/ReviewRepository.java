package com.shophub.backend.repository;

import com.shophub.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    @Modifying(flushAutomatically = true)
    @Query("delete from Review r where r.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}
