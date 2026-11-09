package com.digimate.dto;

import com.digimate.entity.enums.RequestMode;
import com.digimate.validation.NoSensitiveData;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record HelpRequestDto(

        @NotBlank
        @Size(max = 100)
        @NoSensitiveData
        String title,

        @NotBlank
        @Size(max = 1000)
        @NoSensitiveData
        String description,

        @NotNull
        Long categoryId,

        @NotNull
        @Future
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime preferredTime,

        @NotNull
        RequestMode mode,

        @Size(max = 255)
        @NoSensitiveData
        String place
) {
}
