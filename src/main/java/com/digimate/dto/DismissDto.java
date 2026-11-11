package com.digimate.dto;

import com.digimate.validation.NoSensitiveData;
import jakarta.validation.constraints.Size;

public record DismissDto(@Size(max = 500)
                          @NoSensitiveData
                          String note) {
}
