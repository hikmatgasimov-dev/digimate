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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    @WithMockUser(username = "admin@example.test", roles = "ADMIN")
    void shouldRedirectAnAdminAwayFromTheCreateForm() throws Exception {
        mockMvc.perform(get("/requests/new"))
                .andExpect(redirectedUrl("/requests/my"))
                .andExpect(flash().attribute("errorMessage", "Creating requests is not available for admins."));
    }

    @Test
    @WithMockUser(username = "admin@example.test", roles = "ADMIN")
    void shouldNotCreateARequestForAnAdmin() throws Exception {
        String preferredTime = LocalDateTime.now()
                .plusDays(2)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

        mockMvc.perform(post("/requests/new").with(csrf())
                        .param("title", "Set up WhatsApp")
                        .param("description", "Please help me install WhatsApp.")
                        .param("categoryId", "1")
                        .param("preferredTime", preferredTime)
                        .param("mode", "IN_PERSON")
                        .param("place", "City library"))
                .andExpect(redirectedUrl("/requests/my"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "Creating requests is not available for admins."
                ));
        verify(helpRequestService, never()).create(any(), any());

    }
    @Test
    @WithMockUser(username = "anna@example.test", roles = "USER")
    void shouldAllowNormalUserToOpenCreateForm() throws Exception {
        mockMvc.perform(get("/requests/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("request-new"))
                .andExpect(model().attributeExists("form"));
    }
}