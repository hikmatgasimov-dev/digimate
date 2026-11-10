package com.digimate.controller.web;

import com.digimate.dto.DismissDto;
import com.digimate.dto.RemoveHelperDto;
import com.digimate.dto.UserResponse;
import com.digimate.exception.InvalidStatusTransitionException;
import com.digimate.exception.RequestNotFoundException;
import com.digimate.service.ComplaintService;
import com.digimate.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@Hidden
@RequiredArgsConstructor
public class AdminPageController {

    private static final String REDIRECT_HELPERS = "redirect:/admin/helpers";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String NOT_FOUND = "This user or report does not exist.";
    private static final String REASON_PROBLEM =
            "A reason is required to remove a helper. Use up to 500 characters and no ID or card numbers.";
    private static final String NOTE_PROBLEM =
            "The note must be at most 500 characters and must not contain ID or card numbers.";


    private final UserService userService;
    private final ComplaintService complaintService;


    // Helpers waiting for approval, open customer reports, and verified helpers
    @GetMapping("/admin/helpers")
    public String helpers(Model model) {


        model.addAttribute("pending", userService.findPendingHelpers());
        model.addAttribute("reports", complaintService.findOpen());

        model.addAttribute("verified", userService.findPendingHelpers());

        return "admin-helpers";

    }

    // Every user on the platform, without phone numbers

    @GetMapping("/admin/users")
    public String usersModel(Model model) {

        model.addAttribute("users",  userService.findAll());
        return "admin-users";
    }

    // Approve a helper who is waiting
    @PostMapping("/admin/helpers/{id}/verify")
    public String verify(@PathVariable Long id, RedirectAttributes redirectAttributes) {

        try {

            UserResponse helper = userService.verifyHelper(id);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, helper.fullName() + " is now a verified helper.");

        } catch (RequestNotFoundException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_FOUND);
        } catch (InvalidStatusTransitionException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
        }
        return REDIRECT_HELPERS;
    }

    // Remove a helper. The admin must give a reason.
    @PostMapping("/admin/helpers/{id}/remove")
    public String remove(@PathVariable Long id, @Valid @ModelAttribute RemoveHelperDto form,
                         BindingResult result,
                         RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, REASON_PROBLEM);
            return REDIRECT_HELPERS;
        }
        try {
            UserResponse helper = userService.removeHelper(id, form);

            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, helper.fullName() + " was removed as a helper.");
            return REDIRECT_HELPERS;
        } catch (RequestNotFoundException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_FOUND);
        } catch (InvalidStatusTransitionException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
        }
        return REDIRECT_HELPERS;

    }
    // Close a report without action.

    @PostMapping("/admin/complaints/{id}/dismiss")
    public String dismiss(@PathVariable Long id, @Valid @ModelAttribute DismissDto form,
                          BindingResult result,
                          RedirectAttributes redirectAttributes){
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOTE_PROBLEM);
            return REDIRECT_HELPERS;
        }
        try {
             complaintService.dismiss(id,form);

            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Report closed. The helper stays.");
            return REDIRECT_HELPERS;
        } catch (RequestNotFoundException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_FOUND);
        } catch (InvalidStatusTransitionException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
        }
        return REDIRECT_HELPERS;

    }

}
