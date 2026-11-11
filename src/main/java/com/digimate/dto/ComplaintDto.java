package com.digimate.dto;

import com.digimate.validation.NoSensitiveData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComplaintDto (   @NotBlank
                               @Size(max = 500)
                               @NoSensitiveData
                               String reason){


}
