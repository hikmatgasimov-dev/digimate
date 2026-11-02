package com.digimate.dto;

import com.digimate.entity.enums.RequestMode;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record HelpRequestDto(

        @NotBlank
        @Size(max = 100)
        String title,

        @NotBlank
        @Size(max = 1000)
        String description,

        @NotNull
        Long categoryId,

        @NotNull
        @Future
        LocalDateTime preferredTime,

        @NotNull
        RequestMode mode,

        @Size(max = 255)
        String place
) {
}
