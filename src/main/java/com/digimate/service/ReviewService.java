package com.digimate.service;

import com.digimate.dto.HelpRequestResponse;
import com.digimate.dto.ReviewDto;
import com.digimate.entity.HelpRequest;
import com.digimate.entity.Review;
import com.digimate.entity.User;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.exception.DuplicateReviewException;
import com.digimate.exception.InvalidStatusTransitionException;
import com.digimate.exception.RequestNotFoundException;
import com.digimate.repository.HelpRequestRepository;
import com.digimate.repository.ReviewRepository;
import com.digimate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rules for reviews: only the customer who created a COMPLETED request can review it, once.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final HelpRequestRepository helpRequestRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public HelpRequestResponse create(Long requestId, ReviewDto dto, String username) {
        HelpRequest request = helpRequestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Request not found"));
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RequestNotFoundException("User not found"));

        if (request.getCustomer() == null || !request.getCustomer().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the creator can review this request");
        }
        if (request.getStatus() != RequestStatus.COMPLETED) {
            throw new InvalidStatusTransitionException("Only a completed request can be reviewed");
        }
        // The unique column review.request_id is the safety net if two reviews arrive at the same moment.
        if (reviewRepository.existsByRequestId(requestId)) {
            throw new DuplicateReviewException("This request already has a review");
        }

        Review review = new Review();
        review.setRequest(request);
        review.setRating(dto.rating());
        review.setComment(cleanComment(dto.comment()));
        Review saved = reviewRepository.save(review);

        log.info("Review saved: requestId={}, rating={}", requestId, saved.getRating());
        return HelpRequestResponse.from(request, user, saved);
    }

    // An empty or blank comment is stored as "no comment".
    private String cleanComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }
}