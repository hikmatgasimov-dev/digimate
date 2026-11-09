package com.digimate.controller.api;

import com.digimate.config.SecurityConfig;
import com.digimate.dto.HelpRequestResponse;
import com.digimate.dto.ReviewDto;
import com.digimate.entity.enums.RequestMode;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.exception.DuplicateReviewException;
import com.digimate.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewApiController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "anna@example.test")
class ReviewApiControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ReviewService reviewService;

    private static final String BODY = """
            {"rating":5,"comment":"Very kind and patient."}
            """;

    private HelpRequestResponse response() {
        return new HelpRequestResponse(5L, "Set up WhatsApp", "Help me install WhatsApp.", "Phone",
                LocalDateTime.of(2030, 11, 10, 15, 0), RequestMode.ONLINE, null, RequestStatus.COMPLETED,
                Instant.parse("2030-11-01T10:00:00Z"), "Hans Helper", "+4900", "hans@example.test",
                5, "Very kind and patient.");
    }

    @Test
    void shouldSaveAReviewAndReturn201WithTheLoggedInUsername() throws Exception {
        when(reviewService.create(eq(5L), any(ReviewDto.class), eq("anna@example.test"))).thenReturn(response());

        mockMvc.perform(post("/api/requests/5/review").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewRating").value(5))
                .andExpect(jsonPath("$.reviewComment").value("Very kind and patient."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "6", "-1"})
    void shouldReturn400WhenTheRatingIsNotFromOneToFive(String rating) throws Exception {
        mockMvc.perform(post("/api/requests/5/review").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":" + rating + "}"))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).create(any(), any(), any());
    }

    @Test
    void shouldReturn400WhenTheRatingIsMissing() throws Exception {
        mockMvc.perform(post("/api/requests/5/review").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).create(any(), any(), any());
    }

    @Test
    void shouldReturn409WhenTheRequestAlreadyHasAReview() throws Exception {
        when(reviewService.create(eq(5L), any(ReviewDto.class), any()))
                .thenThrow(new DuplicateReviewException("This request already has a review"));

        mockMvc.perform(post("/api/requests/5/review").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn403WhenTheUserIsNotTheCreator() throws Exception {
        when(reviewService.create(eq(5L), any(ReviewDto.class), any()))
                .thenThrow(new AccessDeniedException("Only the creator can review this request"));

        mockMvc.perform(post("/api/requests/5/review").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void shouldReturn401WithoutLogin() throws Exception {
        mockMvc.perform(post("/api/requests/5/review").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reviewService);
    }
    @Test
    void shouldReturn400WhenTheCommentContainsACardNumber() throws Exception {
        mockMvc.perform(post("/api/requests/5/review").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Card 4111 1111 1111 1111\"}"))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).create(any(), any(), any());
    }
}