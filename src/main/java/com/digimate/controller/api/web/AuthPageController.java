package com.digimate.controller.api.web;

import com.digimate.dto.RegisterRequest;
import com.digimate.exception.EmailAlreadyUsedException;
import com.digimate.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
