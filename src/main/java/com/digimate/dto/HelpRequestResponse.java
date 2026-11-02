package com.digimate.dto;

import com.digimate.entity.enums.RequestMode;
import com.digimate.entity.enums.RequestStatus;

import java.time.Instant;
import java.time.LocalDateTime;

public record HelpRequestResponse(
        Long id,
        String title,
        String description,
        String categoryName,
        LocalDateTime preferredTime,
        RequestMode mode,
        String place,
        RequestStatus status,
        Instant createdAt,
        String counterpartName,
        String counterpartPhone,
        String counterpartEmail,
        Integer reviewRating,
        String reviewComment
) {
}
