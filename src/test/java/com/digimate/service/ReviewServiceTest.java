package com.digimate.service;

import com.digimate.dto.HelpRequestResponse;
import com.digimate.dto.ReviewDto;
import com.digimate.entity.Category;
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
import com.digimate.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final String EMAIL = "customer@example.test";

    @Mock HelpRequestRepository helpRequestRepository;
    @Mock UserRepository userRepository;
    @Mock ReviewRepository reviewRepository;
    @InjectMocks ReviewService service;

    private User customer;
    private User helper;
    private HelpRequest request;

    @BeforeEach
    void setUp() {
        customer = TestData.customer();
        customer.setId(1L);
        customer.setEmail(EMAIL);
        helper = TestData.verifiedHelper();
        helper.setId(2L);

        Category category = TestData.category();
        request = TestData.openRequest(customer, category);
        request.setId(100L);
        request.setHelper(helper);
        request.setStatus(RequestStatus.COMPLETED);
    }

    @Test
    void shouldSaveAReviewForACompletedRequest() {
        when(helpRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(reviewRepository.existsByRequestId(100L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HelpRequestResponse response = service.create(100L, new ReviewDto(5, "  Very kind  "), EMAIL);

        assertEquals(5, response.reviewRating());
        assertEquals("Very kind", response.reviewComment());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void shouldStoreABlankCommentAsNoComment() {
        when(helpRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HelpRequestResponse response = service.create(100L, new ReviewDto(4, "   "), EMAIL);

        assertEquals(4, response.reviewRating());
        assertNull(response.reviewComment());
    }

    @Test
    void shouldThrowNotFoundWhenTheRequestIsMissing() {
        ReviewDto dto = new ReviewDto(5, null);
        when(helpRequestRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> service.create(100L, dto, EMAIL));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void shouldRefuseWhenTheReviewerIsNotTheCreator() {
        ReviewDto dto = new ReviewDto(5, null);
        when(helpRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("helper@example.test")).thenReturn(Optional.of(helper));

        assertThrows(AccessDeniedException.class,
                () -> service.create(100L, dto, "helper@example.test"));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void shouldRefuseReviewBeforeTheRequestIsCompleted() {
        ReviewDto dto = new ReviewDto(5, null);
        request.setStatus(RequestStatus.ACCEPTED);
        when(helpRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.create(100L, dto, EMAIL));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void shouldRefuseASecondReviewForTheSameRequest() {
        ReviewDto dto = new ReviewDto(5, null);
        when(helpRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
        when(reviewRepository.existsByRequestId(100L)).thenReturn(true);

        assertThrows(DuplicateReviewException.class,
                () -> service.create(100L, dto, EMAIL));
        verify(reviewRepository, never()).save(any());
    }
}
