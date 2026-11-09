package com.digimate.dto;

import com.digimate.entity.HelpRequest;
import com.digimate.entity.Review;
import com.digimate.entity.User;
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

    public static HelpRequestResponse from(HelpRequest request, User viewer, Review review) {
        User counterpart = counterpartOf(request, viewer);

        return new HelpRequestResponse(
                request.getId(),
                request.getTitle(),
                request.getDescription(),
                request.getCategory().getName(),
                request.getPreferredTime(),
                request.getMode(),
                request.getPlace(),
                request.getStatus(),
                request.getCreatedAt(),
                nameOf(counterpart),
                phoneOf(counterpart),
                emailOf(counterpart),
                review != null ? review.getRating() : null,
                review != null ? review.getComment() : null
        );
    }

    private static User counterpartOf(HelpRequest request, User viewer) {
        // No viewer, or the request is not ACCEPTED/COMPLETED: nobody sees contacts
        if (viewer == null
                || (request.getStatus() != RequestStatus.ACCEPTED
                && request.getStatus() != RequestStatus.COMPLETED)) {
            return null;
        }
        // The customer sees the helper
        if (isSameUser(viewer, request.getCustomer())) {
            return request.getHelper();
        }
        // The helper sees the customer
        if (isSameUser(viewer, request.getHelper())) {
            return request.getCustomer();
        }
        return null;
    }

    private static String nameOf(User user) {
        return user != null ? user.getFullName() : null;
    }

    private static String phoneOf(User user) {
        return user != null ? user.getPhone() : null;
    }

    private static String emailOf(User user) {
        return user != null ? user.getEmail() : null;
    }

    private static boolean isSameUser(User first, User second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.getId() == null) {
            return false;
        }
        return first.getId().equals(second.getId());
    }
}
