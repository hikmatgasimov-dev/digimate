package com.digimate.controller.web;

import com.digimate.dto.HelpRequestDto;
import com.digimate.dto.HelpRequestResponse;
import com.digimate.dto.ReviewDto;
import com.digimate.entity.Category;
import com.digimate.entity.enums.RequestMode;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.exception.DuplicateReviewException;
import com.digimate.exception.InvalidStatusTransitionException;
import com.digimate.exception.RequestNotFoundException;
import com.digimate.repository.CategoryRepository;
import com.digimate.service.HelpRequestService;

import com.digimate.service.ReviewService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@Hidden
@RequiredArgsConstructor
public class RequestPageController {

    private static final String FORM_VIEW = "request-new";
    private static final String REDIRECT_MY_REQUESTS = "redirect:/requests/my";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String REQUEST_NOT_EXIST = "This request does not exist.";
    private static final String ADMIN_CANNOT_CREATE = "Creating requests is not available for admins.";


    private final HelpRequestService helpRequestService;
    private final CategoryRepository categoryRepository;
    private final ReviewService reviewService;

    // Display the form to create a new request
    @GetMapping("/requests/new")
    public String newRequest(Model model,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {

        if (isAdmin(authentication)) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, ADMIN_CANNOT_CREATE);
            return REDIRECT_MY_REQUESTS;
        }

        model.addAttribute("form",
                new HelpRequestDto(
                        null, null, null, null, null, null
                ));

        loadCategories(model);
        addCreateTexts(model);

        return FORM_VIEW;
    }

    // Create a new request
    @PostMapping("/requests/new")
    public String createRequest(
            @Valid @ModelAttribute("form") HelpRequestDto form,
            BindingResult result,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (isAdmin(authentication)) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, ADMIN_CANNOT_CREATE);
            return REDIRECT_MY_REQUESTS;
        }

        // Place is required for in-person requests
        if (form.mode() == RequestMode.IN_PERSON
                && (form.place() == null
                || form.place().isBlank())) {

            result.rejectValue(
                    "place",
                    "place.required",
                    "Place is required for in-person help"
            );
        }

        // Return to the form if validation fails
        if (result.hasErrors()) {
            loadCategories(model);
            addCreateTexts(model);
            return FORM_VIEW;
        }

        helpRequestService.create(
                form,
                authentication.getName()
        );

        redirectAttributes.addFlashAttribute(
                SUCCESS_MESSAGE,
                "Your request was created successfully."
        );

        return REDIRECT_MY_REQUESTS;
    }

    // Display the logged-in customer's requests
    @GetMapping("/requests/my")
    public String myRequests(
            Model model,
            Authentication authentication) {

        model.addAttribute(
                "requests",
                helpRequestService.findMine(
                        authentication.getName()
                )
        );

        return "requests-my";
    }

    @GetMapping("/requests/{id}/edit")
    public String editRequest(
            @PathVariable Long id,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (isAdmin(authentication)) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Changing requests is not available for admins.");
            return REDIRECT_MY_REQUESTS;
        }

        try {
            HelpRequestResponse request = helpRequestService.findById(id, authentication.getName());

            if (request.status() != RequestStatus.OPEN) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "This request can no longer be changed.");
                return REDIRECT_MY_REQUESTS;
            }

            model.addAttribute("form", toForm(request));
            loadCategories(model);
            addEditTexts(model, id);
            return FORM_VIEW;

        } catch (RequestNotFoundException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, REQUEST_NOT_EXIST);
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "You can only change your own requests.");
        }
        return REDIRECT_MY_REQUESTS;
    }

    @PostMapping("/requests/{id}/edit")
    public String updateRequest(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") HelpRequestDto form,
            BindingResult result,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (isAdmin(authentication)) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Changing requests is not available for admins.");
            return REDIRECT_MY_REQUESTS;
        }

        if (form.mode() == RequestMode.IN_PERSON
                && (form.place() == null
                || form.place().isBlank())) {

            result.rejectValue(
                    "place",
                    "place.required",
                    "Place is required for in-person help"
            );
        }

        if (result.hasErrors()) {
            loadCategories(model);
            addEditTexts(model, id);
            return FORM_VIEW;
        }

        try {
            helpRequestService.update(id, form, authentication.getName());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Request updated.");
        } catch (InvalidStatusTransitionException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "This request can no longer be changed.");
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "You can only change your own requests.");
        } catch (RequestNotFoundException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, REQUEST_NOT_EXIST);
        }
        return REDIRECT_MY_REQUESTS;
    }

    // Cancel an existing request
    @PostMapping("/requests/{id}/cancel")
    public String cancelRequest(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            helpRequestService.cancel(id, authentication.getName());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Your request was cancelled.");
        } catch (InvalidStatusTransitionException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "This request can no longer be cancelled.");
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "You can only cancel your own requests.");
        } catch (RequestNotFoundException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, REQUEST_NOT_EXIST);
        }

        return REDIRECT_MY_REQUESTS;
    }

    // Load categories for the request form
    private void loadCategories(Model model) {

        model.addAttribute(
                "categories",
                categoryRepository.findAll(
                        Sort.by("name")
                )
        );
    }

    private void addCreateTexts(Model model) {
        model.addAttribute("formTitle", "Ask for help");
        model.addAttribute("formAction", "/requests/new");
        model.addAttribute("submitLabel", "Send request");
    }

    private void addEditTexts(Model model, Long id) {
        model.addAttribute("formTitle", "Edit request");
        model.addAttribute("formAction", "/requests/" + id + "/edit");
        model.addAttribute("submitLabel", "Save changes");
    }

    private HelpRequestDto toForm(HelpRequestResponse request) {
        List<Category> categories = categoryRepository.findAll();
        Long categoryId = categories.stream()
                .filter(category -> category.getName().equals(request.categoryName()))
                .map(Category::getId)
                .findFirst()
                .orElse(null);

        return new HelpRequestDto(
                request.title(),
                request.description(),
                categoryId,
                request.preferredTime(),
                request.mode(),
                request.place()
        );
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    // Rate a completed request (the form is in requests-my.html)
    @PostMapping("/requests/{id}/review")
    public String reviewRequest(
            @PathVariable Long id,
            @Valid @ModelAttribute ReviewDto form,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please choose a rating from 1 to 5.");
            return REDIRECT_MY_REQUESTS;
        }

        try {
            reviewService.create(id, form, authentication.getName());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Thank you. Your review is saved.");
        } catch (DuplicateReviewException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "This request already has a review.");
        } catch (InvalidStatusTransitionException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "You can review a request only after it is completed.");
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "You can only review your own requests.");
        } catch (RequestNotFoundException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, REQUEST_NOT_EXIST);
        }
        return REDIRECT_MY_REQUESTS;
    }
}
