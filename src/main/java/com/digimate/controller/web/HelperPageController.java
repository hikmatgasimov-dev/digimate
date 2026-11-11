package com.digimate.controller.web;

import com.digimate.dto.HelpRequestResponse;
import com.digimate.exception.HelperNotVerifiedException;
import com.digimate.exception.InvalidStatusTransitionException;
import com.digimate.exception.RequestAlreadyTakenException;
import com.digimate.exception.RequestNotFoundException;
import com.digimate.service.HelperFlowService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/* The helper inbox page: open requests, the requests a helper accepted,
 * and the buttons to accept and to complete.
 */
@Controller
@Hidden
@RequiredArgsConstructor
public class HelperPageController {

    private static final String REDIRECT_HELPER = "redirect:/helper";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String REQUEST_NOT_EXIST = "This request does not exist.";
    private static final String JUST_ACCEPTED =
            "This request was just accepted by another helper. Please choose another one.";

    private final HelperFlowService helperFlowService;

    // Show the open requests and the requests this helper accepted
    @GetMapping("/helper")
    public String helperInbox(Model model, Authentication authentication) {
        try {

            String username = authentication.getName();
            List<HelpRequestResponse> open = helperFlowService.findOpen(username);
            List<HelpRequestResponse> mine = helperFlowService.findHelping(username);

            model.addAttribute("openRequests", open);
            model.addAttribute("myRequests", mine);

        } catch (HelperNotVerifiedException e) {
            // No lists: only the sentence that tells the person what to do next.
            model.addAttribute(ERROR_MESSAGE, e.getMessage());
        }
        return "helper-inbox";
    }

    // Accept an open request
    @PostMapping("/helper/requests/{id}/accept")
    public String accept(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            helperFlowService.accept(id, authentication.getName());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "You accepted this request. The customer's contact details are shown below.");

        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, JUST_ACCEPTED);
        } catch (RequestNotFoundException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, REQUEST_NOT_EXIST);
        } catch (HelperNotVerifiedException | RequestAlreadyTakenException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
        }
        return REDIRECT_HELPER;

    }
    // Mark an accepted request as completed
    @PostMapping("/helper/requests/{id}/complete")
    public String complete(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes){
        try{
        helperFlowService.complete(id,authentication.getName());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                    "Marked as completed. The customer can now write a review.");
        }
        catch (RequestNotFoundException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, REQUEST_NOT_EXIST);
        } catch (InvalidStatusTransitionException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
        }
        return REDIRECT_HELPER;
    }
}

