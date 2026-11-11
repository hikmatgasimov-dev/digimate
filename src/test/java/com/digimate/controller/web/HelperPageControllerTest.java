package com.digimate.controller.web;

import com.digimate.config.SecurityConfig;
import com.digimate.dto.HelpRequestResponse;
import com.digimate.entity.HelpRequest;
import com.digimate.entity.enums.RequestMode;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.exception.HelperNotVerifiedException;
import com.digimate.exception.InvalidStatusTransitionException;
import com.digimate.exception.RequestAlreadyTakenException;
import com.digimate.exception.RequestNotFoundException;
import com.digimate.service.HelperFlowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HelperPageController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "helper@example.test")
class HelperPageControllerTest {

    private static final String JUST_ACCEPTED =
            "This request was just accepted by another helper. Please choose another one.";

    @Autowired MockMvc mockMvc;
    @MockitoBean HelperFlowService helperFlowService;

    // ---- the inbox page ----

    @Test
    void shouldShowTheOpenAndTheAcceptedRequestsToAVerifiedHelper() throws Exception {
        when(helperFlowService.findOpen("helper@example.test"))
                .thenReturn(List.of(response(1L, "Set up WhatsApp", RequestStatus.OPEN)));
        when(helperFlowService.findHelping("helper@example.test"))
                .thenReturn(List.of(response(2L, "Pay bills online", RequestStatus.ACCEPTED)));

        mockMvc.perform(get("/helper"))
                .andExpect(status().isOk())
                .andExpect(view().name("helper-inbox"))
                .andExpect(model().attributeExists("openRequests", "myRequests"))
                .andExpect(content().string(containsString("Set up WhatsApp")))
                .andExpect(content().string(containsString("Pay bills online")));
    }

    @Test
    void shouldShowOnlyTheMessageToAHelperWhoIsNotVerified() throws Exception {
        when(helperFlowService.findOpen(any()))
                .thenThrow(new HelperNotVerifiedException("Your helper account is waiting for approval"));

        mockMvc.perform(get("/helper"))
                .andExpect(status().isOk())
                .andExpect(view().name("helper-inbox"))
                .andExpect(model().attribute("errorMessage", "Your helper account is waiting for approval"))
                .andExpect(model().attributeDoesNotExist("openRequests", "myRequests"))
                .andExpect(content().string(containsString("Your helper account is waiting for approval")));
    }

    // ---- accept ----

    @Test
    void shouldAcceptTheRequestAndGoBackToTheInbox() throws Exception {
        mockMvc.perform(post("/helper/requests/7/accept").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/helper"))
                .andExpect(flash().attribute("successMessage",
                        "You accepted this request. The customer's contact details are shown below."));

        verify(helperFlowService).accept(7L, "helper@example.test");
    }

    @Test
    void shouldTellTheSecondHelperThatAnotherHelperWasFirst() throws Exception {
        when(helperFlowService.accept(eq(7L), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(HelpRequest.class, 7L));

        mockMvc.perform(post("/helper/requests/7/accept").with(csrf()))
                .andExpect(redirectedUrl("/helper"))
                .andExpect(flash().attribute("errorMessage", JUST_ACCEPTED));
    }

    @Test
    void shouldShowTheMessageWhenTheRequestIsNoLongerOpen() throws Exception {
        when(helperFlowService.accept(eq(7L), any()))
                .thenThrow(new RequestAlreadyTakenException("This request is no longer open"));

        mockMvc.perform(post("/helper/requests/7/accept").with(csrf()))
                .andExpect(redirectedUrl("/helper"))
                .andExpect(flash().attribute("errorMessage", "This request is no longer open"));
    }

    @Test
    void shouldRefuseToAcceptTheHelpersOwnRequest() throws Exception {
        when(helperFlowService.accept(eq(7L), any()))
                .thenThrow(new AccessDeniedException("You cannot accept your own request"));

        mockMvc.perform(post("/helper/requests/7/accept").with(csrf()))
                .andExpect(redirectedUrl("/helper"))
                .andExpect(flash().attribute("errorMessage", "You cannot accept your own request"));
    }

    @Test
    void shouldShowAMessageWhenTheRequestToAcceptDoesNotExist() throws Exception {
        when(helperFlowService.accept(eq(99L), any()))
                .thenThrow(new RequestNotFoundException("Request not found"));

        mockMvc.perform(post("/helper/requests/99/accept").with(csrf()))
                .andExpect(redirectedUrl("/helper"))
                .andExpect(flash().attribute("errorMessage", "This request does not exist."));
    }

    @Test
    void shouldShowTheReasonWhenAHelperWhoIsNotVerifiedTriesToAccept() throws Exception {
        when(helperFlowService.accept(eq(7L), any()))
                .thenThrow(new HelperNotVerifiedException("An admin removed your helper account"));

        mockMvc.perform(post("/helper/requests/7/accept").with(csrf()))
                .andExpect(redirectedUrl("/helper"))
                .andExpect(flash().attribute("errorMessage", "An admin removed your helper account"));
    }

    // ---- complete ----

    @Test
    void shouldCompleteTheRequestAndGoBackToTheInbox() throws Exception {
        mockMvc.perform(post("/helper/requests/7/complete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/helper"))
                .andExpect(flash().attribute("successMessage",
                        "Marked as completed. The customer can now write a review."));

        verify(helperFlowService).complete(7L, "helper@example.test");
    }

    @Test
    void shouldRefuseToCompleteARequestThatIsNotAccepted() throws Exception {
        when(helperFlowService.complete(eq(7L), any()))
                .thenThrow(new InvalidStatusTransitionException("Only an accepted request can be completed"));

        mockMvc.perform(post("/helper/requests/7/complete").with(csrf()))
                .andExpect(redirectedUrl("/helper"))
                .andExpect(flash().attribute("errorMessage", "Only an accepted request can be completed"));
    }

    @Test
    void shouldRefuseToCompleteSomeoneElsesRequest() throws Exception {
        when(helperFlowService.complete(eq(7L), any()))
                .thenThrow(new AccessDeniedException("Only the assigned helper can complete this request"));

        mockMvc.perform(post("/helper/requests/7/complete").with(csrf()))
                .andExpect(redirectedUrl("/helper"))
                .andExpect(flash().attribute("errorMessage", "Only the assigned helper can complete this request"));
    }

    // ---- security ----

    @Test
    void shouldRejectAnAcceptWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/helper/requests/7/accept"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(helperFlowService);
    }

    private static HelpRequestResponse response(Long id, String title, RequestStatus status) {
        return new HelpRequestResponse(id, title, "Please help me.", "Phone",
                LocalDateTime.now().plusDays(1), RequestMode.ONLINE, null, status,
                Instant.now(), null, null, null, null, null);
    }
}