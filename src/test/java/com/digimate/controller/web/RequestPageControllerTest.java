package com.digimate.controller.web;

import com.digimate.config.SecurityConfig;
import com.digimate.dto.ReviewDto;
import com.digimate.exception.DuplicateReviewException;
import com.digimate.repository.CategoryRepository;
import com.digimate.service.HelpRequestService;
import com.digimate.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestPageController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "anna@example.test")
class RequestPageControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean HelpRequestService helpRequestService;
    @MockitoBean ReviewService reviewService;
    @MockitoBean CategoryRepository categoryRepository;

    @Test
    void shouldSaveTheReviewAndGoBackToMyRequests() throws Exception {
        mockMvc.perform(post("/requests/5/review").with(csrf()).param("rating", "5").param("comment", "Very kind"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/requests/my"))
                .andExpect(flash().attribute("successMessage", "Thank you. Your review is saved."));

        verify(reviewService).create(eq(5L), any(ReviewDto.class), eq("anna@example.test"));
    }

    @Test
    void shouldShowAMessageWhenTheRequestAlreadyHasAReview() throws Exception {
        when(reviewService.create(eq(5L), any(ReviewDto.class), any()))
                .thenThrow(new DuplicateReviewException("This request already has a review"));

        mockMvc.perform(post("/requests/5/review").with(csrf()).param("rating", "4"))
                .andExpect(redirectedUrl("/requests/my"))
                .andExpect(flash().attribute("errorMessage", "This request already has a review."));
    }

    @Test
    void shouldRefuseAReviewWithoutARating() throws Exception {
        mockMvc.perform(post("/requests/5/review").with(csrf()))
                .andExpect(redirectedUrl("/requests/my"))
                .andExpect(flash().attribute("errorMessage", "Please choose a rating from 1 to 5."));

        verify(reviewService, never()).create(any(), any(), any());
    }

    @Test
    void shouldRejectAReviewPostWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/requests/5/review").param("rating", "5"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reviewService);
    }
}