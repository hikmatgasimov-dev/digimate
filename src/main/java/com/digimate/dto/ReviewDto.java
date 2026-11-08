package com.digimate.dto;

import com.digimate.validation.NoSensitiveData;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Input for a review: a rating from 1 to 5 and an optional comment.
 * The checks are on the fields, so wrong values are refused before the service runs.
 */
public record ReviewDto(

        @NotNull
        @Min(1)
        @Max(5)
        Integer rating,

        @Size(max = 1000)
        @NoSensitiveData
        String comment
) {
}