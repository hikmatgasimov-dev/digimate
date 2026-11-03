package com.digimate.dto;

import com.digimate.entity.User;
import com.digimate.entity.enums.Role;
import com.digimate.entity.enums.HelperStatus;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        HelperStatus helperStatus
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getHelperStatus()
        );
    }
}
