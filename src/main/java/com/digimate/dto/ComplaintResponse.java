package com.digimate.dto;

import com.digimate.entity.enums.ComplaintStatus;

import java.time.Instant;

/**
 * One customer report, as the admin sees it.
 * It has the helper's name and email and the customer's first name only.
 * It never has a phone number.
 */
public record ComplaintResponse(
        Long id,
        Long requestId,
        Long helperId,
        String helperName,
        String helperEmail,
        String customerFirstName,
        String text,
        ComplaintStatus status,
        Instant createdAt
) {
}