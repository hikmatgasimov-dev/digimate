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

import java.util.List;

/**
 * Admin endpoints. The security chain already requires ROLE_ADMIN for /api/admin/**.
 * The controller only translates HTTP; the rules are in UserService.
 */


@RestController
@RequestMapping("api/admin/helpers")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Helper verification (ADMIN only)")
public class AdminApiController {
    private final UserService userService;


    @GetMapping("/pending")
    @Operation(summary = "List helpers waiting for verification")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The pending helpers"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })

    public List<UserResponse> pendingHelpers() {
        return userService.findPendingHelpers();
    }

    @PutMapping("/{id}/verify")
    @Operation(summary = "verify pending helper")
    @ApiResponses({

            @ApiResponse(responseCode = "200", description= "The helper is now VERIFIED"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not an admin"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "The user is not a pending helper")
    })

    public UserResponse verifyHelper(@PathVariable Long id){
        return userService.verifyHelper(id);
    }

}
