package com.digimate.controller.web;

import com.digimate.dto.RegisterRequest;
import com.digimate.exception.EmailAlreadyUsedException;
import com.digimate.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@Hidden
@RequiredArgsConstructor
public class AuthPageController {

    private final UserService userService;

    // The brand link in the menu points to "/". Guests are sent to /login by the security chain.
    // The brand link in the menu and the login both lead to "/". Guests are sent to /login by the security chain.
    // An admin goes to the helpers page, everyone else to My requests.
    @GetMapping("/")
    public String home(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return admin ? "redirect:/admin/helpers" : "redirect:/requests/my";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }


    @GetMapping("/register")
    public String showRegister(Model model) {

        model.addAttribute(
                "form",
                new RegisterRequest("", "", "", "", false)
        );

        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") RegisterRequest form,
            BindingResult result) {

        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.register(form);

        } catch (EmailAlreadyUsedException ex) {

            result.rejectValue(
                    "email",
                    "email.duplicate",
                    ex.getMessage()
            );

            return "register";
        }

        return "redirect:/login?registered";
    }
}
