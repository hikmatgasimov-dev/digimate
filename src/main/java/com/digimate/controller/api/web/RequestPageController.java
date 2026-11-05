package com.digimate.controller.api.web;

import com.digimate.dto.HelpRequestDto;
import com.digimate.entity.enums.RequestMode;
import com.digimate.repository.CategoryRepository;
import com.digimate.service.HelpRequestService;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Hidden
@RequiredArgsConstructor
public class RequestPageController {

    private final HelpRequestService helpRequestService;
    private final CategoryRepository categoryRepository;

    // Display the form to create a new request
    @GetMapping("/requests/new")
    public String newRequest(Model model) {

        model.addAttribute("form",
                new HelpRequestDto(
                        null, null, null, null, null, null
                ));

        loadCategories(model);

        return "request-new";
    }

    // Create a new request
    @PostMapping("/requests/new")
    public String createRequest(
            @Valid @ModelAttribute("form") HelpRequestDto form,
            BindingResult result,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

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
            return "request-new";
        }

        helpRequestService.create(
                form,
                authentication.getName()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your request was created successfully."
        );

        return "redirect:/requests/my";
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

    // Cancel an existing request
    @PostMapping("/requests/{id}/cancel")
    public String cancelRequest(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        helpRequestService.cancel(
                id,
                authentication.getName()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your request was cancelled."
        );

        return "redirect:/requests/my";
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

    // TODO: Add the review POST endpoint when
    // Hikmat's ReviewService is available.
}
