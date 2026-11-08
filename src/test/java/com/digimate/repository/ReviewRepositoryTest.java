package com.digimate.repository;

import com.digimate.entity.Category;
import com.digimate.entity.HelpRequest;
import com.digimate.entity.Review;
import com.digimate.entity.User;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.support.TestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/** Integration tests for per-request review lookup and uniqueness. */
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private HelpRequestRepository helpRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldFindReviewForRequestedRequest() {
        HelpRequest request = saveCompletedRequest();
        HelpRequest otherRequest = saveRequest(request.getCustomer(), request.getCategory());
        Review expected = reviewRepository.save(review(request, 5, "Patient and clear explanation."));
        reviewRepository.saveAndFlush(review(otherRequest, 3, "Helpful session."));
        entityManager.clear();

        Review found = reviewRepository.findByRequestId(request.getId()).orElseThrow();

        assertEquals(expected.getId(), found.getId());
        assertEquals(request.getId(), found.getRequest().getId());
        assertEquals(5, found.getRating());
        assertEquals("Patient and clear explanation.", found.getComment());
    }

    @Test
    void shouldReturnEmptyWhenOnlyAnotherRequestHasReview() {
        HelpRequest reviewed = saveCompletedRequest();
        HelpRequest unreviewed = saveRequest(reviewed.getCustomer(), reviewed.getCategory());
        reviewRepository.saveAndFlush(review(reviewed, 4, "Clear instructions."));
        entityManager.clear();

        Optional<Review> found = reviewRepository.findByRequestId(unreviewed.getId());

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReportReviewExistsForRequestedRequest() {
        HelpRequest request = saveCompletedRequest();
        reviewRepository.saveAndFlush(review(request, 5, "Thank you."));
        entityManager.clear();

        boolean exists = reviewRepository.existsByRequestId(request.getId());

        assertTrue(exists);
    }

    @Test
    void shouldReportNoReviewWhenOnlyAnotherRequestHasReview() {
        HelpRequest reviewed = saveCompletedRequest();
        HelpRequest unreviewed = saveRequest(reviewed.getCustomer(), reviewed.getCategory());
        reviewRepository.saveAndFlush(review(reviewed, 4, "Clear instructions."));
        entityManager.clear();

        boolean exists = reviewRepository.existsByRequestId(unreviewed.getId());

        assertFalse(exists);
    }

    @Test
    void shouldReturnEmptyWhenRequestDoesNotExist() {
        Optional<Review> found = reviewRepository.findByRequestId(Long.MAX_VALUE);

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldRejectSecondReviewForSameRequest() {
        HelpRequest request = saveCompletedRequest();
        reviewRepository.saveAndFlush(review(request, 5, "First review."));
        Review duplicate = review(request, 1, "Second review.");

        assertThrows(DataIntegrityViolationException.class, () -> reviewRepository.saveAndFlush(duplicate));
    }

    private HelpRequest saveCompletedRequest() {
        User customer = userRepository.save(TestData.customer());
        Category category = categoryRepository.save(TestData.category());
        return saveRequest(customer, category);
    }

    private HelpRequest saveRequest(User customer, Category category) {
        HelpRequest request = TestData.openRequest(customer, category);
        request.setStatus(RequestStatus.COMPLETED);
        return helpRequestRepository.saveAndFlush(request);
    }

    private Review review(HelpRequest request, int rating, String comment) {
        Review review = new Review();
        review.setRequest(request);
        review.setRating(rating);
        review.setComment(comment);
        return review;
    }
}
