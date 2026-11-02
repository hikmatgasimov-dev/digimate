package com.digimate.repository;

import com.digimate.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByRequestId(Long requestId);

    Optional<Review> findByRequestId(Long requestId);
}
