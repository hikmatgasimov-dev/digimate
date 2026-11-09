package com.digimate.controller.api;

import com.digimate.config.SecurityConfig;
import com.digimate.dto.UserResponse;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.Role;
import com.digimate.exception.InvalidStatusTransitionException;
import com.digimate.exception.RequestNotFoundException;
import com.digimate.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MVC tests for admin endpoints: access control, response bodies
 * and error handling, including an empty pending-helper list.
 */

@WebMvcTest(AdminApiController.class)
@Import(SecurityConfig.class)
class AdminApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private JsonMapper jsonMapper;

    private static final Long VALID_HELPER_ID = 1L;

    @Test
    void shouldReturn401WhenAnonymousListsPendingHelpers() throws Exception {
        mockMvc.perform(get("/api/admin/helpers/pending"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn401WhenAnonymousVerifiesHelper() throws Exception {
        mockMvc.perform(put("/api/admin/helpers/{id}/verify", VALID_HELPER_ID))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403WhenUserListsPendingHelpers() throws Exception {
        mockMvc.perform(get("/api/admin/helpers/pending"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403WhenUserVerifiesHelper() throws Exception {
        mockMvc.perform(put("/api/admin/helpers/{id}/verify", VALID_HELPER_ID))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnPendingHelpersWhenAdmin() throws Exception {
        List<UserResponse> pendingHelpers = List.of(
                new UserResponse(
                        VALID_HELPER_ID, "Anna Novak", "anna.novak@example.com", Role.USER, HelperStatus.PENDING),
                new UserResponse(
                        2L, "Markus Berg", "markus.berg@example.com", Role.USER, HelperStatus.PENDING),
                new UserResponse(
                        3L, "Elena Petrova", "elena.petrova@example.com", Role.USER, HelperStatus.PENDING),
                new UserResponse(
                        4L, "Daniel Reed", "daniel.reed@example.com", Role.USER, HelperStatus.PENDING));

        String expectedJson = jsonMapper.writeValueAsString(pendingHelpers);

        when(userService.findPendingHelpers()).thenReturn(pendingHelpers);

        mockMvc.perform(get("/api/admin/helpers/pending"))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));

        verify(userService).findPendingHelpers();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200WhenAdminVerifiesPendingHelper() throws Exception {
        UserResponse verifiedHelper = new UserResponse(
                VALID_HELPER_ID,
                "Anna Novak",
                "anna.novak@example.com",
                Role.USER,
                HelperStatus.VERIFIED);

        when(userService.verifyHelper(VALID_HELPER_ID)).thenReturn(verifiedHelper);

        mockMvc.perform(put("/api/admin/helpers/{id}/verify", VALID_HELPER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VALID_HELPER_ID))
                .andExpect(jsonPath("$.helperStatus").value("VERIFIED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn409WhenHelperIsNotPending() throws Exception {
        String errorMessage = "Only a pending helper can be verified.";
        when(userService.verifyHelper(VALID_HELPER_ID))
                .thenThrow(new InvalidStatusTransitionException(errorMessage));

        mockMvc.perform(put("/api/admin/helpers/{id}/verify", VALID_HELPER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(errorMessage))
                .andExpect(jsonPath("$.path").value("/api/admin/helpers/" + VALID_HELPER_ID + "/verify"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.passwordHash").doesNotHaveJsonPath());

        verify(userService).verifyHelper(VALID_HELPER_ID);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404WhenHelperDoesNotExist() throws Exception {
        String errorMessage = "User not found";
        when(userService.verifyHelper(VALID_HELPER_ID))
                .thenThrow(new RequestNotFoundException(errorMessage));

        mockMvc.perform(put("/api/admin/helpers/{id}/verify", VALID_HELPER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(errorMessage))
                .andExpect(jsonPath("$.path").value("/api/admin/helpers/" + VALID_HELPER_ID + "/verify"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.passwordHash").doesNotHaveJsonPath());

        verify(userService).verifyHelper(VALID_HELPER_ID);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnEmptyPendingListWhenAdmin() throws Exception {
        when(userService.findPendingHelpers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/helpers/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
