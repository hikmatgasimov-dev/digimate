package com.digimate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Size(max = 100)
        String fullName,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(max = 30)
        String phone,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        boolean wantsToHelp

) {
}
