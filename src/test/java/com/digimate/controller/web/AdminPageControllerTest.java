package com.digimate.controller.web;

import com.digimate.config.SecurityConfig;
import com.digimate.dto.DismissDto;
import com.digimate.dto.RemoveHelperDto;
import com.digimate.dto.UserResponse;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.Role;
import com.digimate.exception.InvalidStatusTransitionException;
import com.digimate.exception.RequestNotFoundException;
import com.digimate.service.ComplaintService;
import com.digimate.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.ViewResolver;

import java.util.List;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminPageController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class AdminPageControllerTest {

    private static final String REASON_PROBLEM =
            "A reason is required to remove a helper. Use up to 500 characters and no ID or card numbers.";
    private static final String NOTE_PROBLEM =
            "The note must be at most 500 characters and must not contain ID or card numbers.";

    @Autowired MockMvc mockMvc;
    @MockitoBean UserService userService;
    @MockitoBean ComplaintService complaintService;

    /**
     * These tests check the controller, not the page design, so the real templates are not needed.
     * This replaces Thymeleaf's view resolver with one that renders nothing.
     */
    @TestConfiguration
    static class WithoutTemplates {
        @Bean
        ViewResolver thymeleafViewResolver() {
            return (viewName, locale) -> (model, request, response) -> { };
        }
    }

    // ---- who can open the pages ----

    @Test
    @WithMockUser(roles = "USER")
    void shouldRefuseANormalUserOnTheHelpersPage() throws Exception {
        mockMvc.perform(get("/admin/helpers"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService, complaintService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldRefuseANormalUserOnTheUsersPage() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    // ---- the pages ----

    @Test
    void shouldShowTheWaitingHelpersOnTheHelpersPage() throws Exception {
        List<UserResponse> pending = List.of(user(3L, "Pending Helper", HelperStatus.PENDING));
        when(userService.findPendingHelpers()).thenReturn(pending);

        mockMvc.perform(get("/admin/helpers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-helpers"))
                .andExpect(model().attribute("pending", pending))
                // empty for now: reports and verified helpers come with the complaint and removal cards
                .andExpect(model().attribute("reports", List.of()))
                .andExpect(model().attribute("verified", List.of()));
    }

    @Test
    void shouldOpenTheUsersPage() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-users"))
                // empty for now: the list of all users comes with its own card
                .andExpect(model().attribute("users", List.of()));
    }

    // ---- verify ----

    @Test
    void shouldVerifyAHelperAndGoBackToTheHelpersPage() throws Exception {
        when(userService.verifyHelper(3L)).thenReturn(user(3L, "Pending Helper", HelperStatus.VERIFIED));

        mockMvc.perform(post("/admin/helpers/3/verify").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("successMessage", "Pending Helper is now a verified helper."));
    }

    @Test
    void shouldShowAMessageWhenTheHelperToVerifyDoesNotExist() throws Exception {
        when(userService.verifyHelper(99L)).thenThrow(new RequestNotFoundException("User not found"));

        mockMvc.perform(post("/admin/helpers/99/verify").with(csrf()))
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("errorMessage", "This user or report does not exist."));
    }

    @Test
    void shouldShowAMessageWhenTheUserIsNotWaitingForApproval() throws Exception {
        when(userService.verifyHelper(3L))
                .thenThrow(new InvalidStatusTransitionException("Only a pending helper can be verified."));

        mockMvc.perform(post("/admin/helpers/3/verify").with(csrf()))
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("errorMessage", "Only a pending helper can be verified."));
    }

    // ---- remove ----

    @Test
    void shouldRemoveAHelperAndGoBackToTheHelpersPage() throws Exception {
        when(userService.removeHelper(eq(2L), any(RemoveHelperDto.class)))
                .thenReturn(user(2L, "Helper One", HelperStatus.REMOVED));

        mockMvc.perform(post("/admin/helpers/2/remove").with(csrf()).param("reason", "Did not show up"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("successMessage", "Helper One was removed as a helper."));

        verify(userService).removeHelper(2L, new RemoveHelperDto("Did not show up"));
    }

    @Test
    void shouldAskForAReasonWhenTheReasonIsBlank() throws Exception {
        mockMvc.perform(post("/admin/helpers/2/remove").with(csrf()).param("reason", "   "))
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("errorMessage", REASON_PROBLEM));

        verify(userService, never()).removeHelper(any(), any());
    }

    @Test
    void shouldAskForAReasonWhenTheReasonIsMissing() throws Exception {
        mockMvc.perform(post("/admin/helpers/2/remove").with(csrf()))
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("errorMessage", REASON_PROBLEM));

        verify(userService, never()).removeHelper(any(), any());
    }

    @Test
    void shouldRefuseARemovalReasonWithACardNumber() throws Exception {
        mockMvc.perform(post("/admin/helpers/2/remove").with(csrf()).param("reason", "Paid with 4111 1111 1111 1111"))
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("errorMessage", REASON_PROBLEM));

        verify(userService, never()).removeHelper(any(), any());
    }

    @Test
    void shouldRefuseARemovalReasonThatIsTooLong() throws Exception {
        mockMvc.perform(post("/admin/helpers/2/remove").with(csrf()).param("reason", "a".repeat(501)))
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("errorMessage", REASON_PROBLEM));

        verify(userService, never()).removeHelper(any(), any());
    }

    @Test
    void shouldShowAMessageWhenTheHelperToRemoveIsNotVerified() throws Exception {
        when(userService.removeHelper(eq(2L), any(RemoveHelperDto.class)))
                .thenThrow(new InvalidStatusTransitionException("Only a verified helper can be removed."));

        mockMvc.perform(post("/admin/helpers/2/remove").with(csrf()).param("reason", "Did not show up"))
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("errorMessage", "Only a verified helper can be removed."));
    }

    @Test
    void shouldShowAMessageWhenTheHelperToRemoveDoesNotExist() throws Exception {
        when(userService.removeHelper(eq(99L), any(RemoveHelperDto.class)))
                .thenThrow(new RequestNotFoundException("User not found"));

        mockMvc.perform(post("/admin/helpers/99/remove").with(csrf()).param("reason", "Did not show up"))
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("errorMessage", "This user or report does not exist."));
    }

    // ---- dismiss ----

    @Test
    void shouldDismissAReportWithANote() throws Exception {
        mockMvc.perform(post("/admin/complaints/1/dismiss").with(csrf()).param("note", "Misunderstanding"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("successMessage", "Report closed. The helper stays."));

        verify(complaintService).dismiss(1L, new DismissDto("Misunderstanding"));
    }

    @Test
    void shouldDismissAReportWithoutANote() throws Exception {
        mockMvc.perform(post("/admin/complaints/1/dismiss").with(csrf()))
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("successMessage", "Report closed. The helper stays."));

        verify(complaintService).dismiss(1L, new DismissDto(null));
    }

    @Test
    void shouldShowAMessageWhenTheReportIsAlreadyClosed() throws Exception {
        when(complaintService.dismiss(eq(1L), any(DismissDto.class)))
                .thenThrow(new InvalidStatusTransitionException("This report is already closed."));

        mockMvc.perform(post("/admin/complaints/1/dismiss").with(csrf()))
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("errorMessage", "This report is already closed."));
    }

    @Test
    void shouldShowAMessageWhenTheReportDoesNotExist() throws Exception {
        when(complaintService.dismiss(eq(99L), any(DismissDto.class)))
                .thenThrow(new RequestNotFoundException("Report not found"));

        mockMvc.perform(post("/admin/complaints/99/dismiss").with(csrf()))
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("errorMessage", "This user or report does not exist."));
    }

    @Test
    void shouldRefuseADismissNoteWithACardNumber() throws Exception {
        mockMvc.perform(post("/admin/complaints/1/dismiss").with(csrf()).param("note", "Card 4111 1111 1111 1111"))
                .andExpect(redirectedUrl("/admin/helpers"))
                .andExpect(flash().attribute("errorMessage", NOTE_PROBLEM));

        verify(complaintService, never()).dismiss(any(), any());
    }

    // ---- security ----

    @Test
    void shouldRejectAPostWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/admin/helpers/3/verify"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService, complaintService);
    }

    private static UserResponse user(Long id, String name, HelperStatus helperStatus) {
        return new UserResponse(id, name, name.toLowerCase().replace(' ', '.') + "@digimate.test", Role.USER, helperStatus);
    }
}