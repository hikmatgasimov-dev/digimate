package com.digimate.controller.api;


import com.digimate.dto.UserResponse;
import com.digimate.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.digimate.dto.RemoveHelperDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Admin endpoints. The security chain already requires ROLE_ADMIN for /api/admin/**.
 * The controller only translates HTTP; the rules are in UserService.
 */


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Helper verification and removal, and the list of users (ADMIN only)")
public class AdminApiController {
    private final UserService userService;


    @GetMapping("/helpers/pending")
    @Operation(summary = "List helpers waiting for verification")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The pending helpers"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })

    public List<UserResponse> pendingHelpers() {
        return userService.findPendingHelpers();
    }

    @PutMapping("/helpers/{id}/verify")
    @Operation(summary = "verify pending helper")
    @ApiResponses({

            @ApiResponse(responseCode = "200", description = "The helper is now VERIFIED"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not an admin"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "The user is not a pending helper")
    })

    public UserResponse verifyHelper(@PathVariable Long id) {
        return userService.verifyHelper(id);
    }


    @PutMapping("/helpers/{id}/remove")
    @Operation(summary = "Remove a verified helper (permanent). Their accepted requests become open again.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The helper is now REMOVED"),
            @ApiResponse(responseCode = "400", description = "The reason is blank, too long, or has an ID or card number"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not an admin"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "The user is not a verified helper")
    })
    public UserResponse removeHelper(@PathVariable Long id, @Valid @RequestBody RemoveHelperDto dto) {
        return userService.removeHelper(id, dto);
    }

    @GetMapping("/helpers/verified")
    @Operation(summary = "List the verified helpers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The verified helpers"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    public List<UserResponse> verifiedHelpers() {

        return userService.findVerifiedHelpers();
    }


    @GetMapping("/users")
    @Operation(summary = "List every user (no phone numbers, no passwords)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All users, by name"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    public List<UserResponse> allUsers() {

        return userService.findAll();
    }
}
