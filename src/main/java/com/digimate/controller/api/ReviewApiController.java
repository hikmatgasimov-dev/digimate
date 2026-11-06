package com.digimate.controller.api;

import com.digimate.dto.HelpRequestResponse;
import com.digimate.dto.ReviewDto;
import com.digimate.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Review endpoint. The controller only passes the logged-in email and the body to the service.
 * All rules live in ReviewService.
 */
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Rate the help after a request is completed (creator only)")
public class ReviewApiController {

    private final ReviewService reviewService;

    @PostMapping("/{id}/review")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Rate a completed request (only the creator, once)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "The review was saved"),
            @ApiResponse(responseCode = "400", description = "Rating is missing or not from 1 to 5"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not the creator of the request"),
            @ApiResponse(responseCode = "404", description = "Request not found"),
            @ApiResponse(responseCode = "409", description = "Request is not completed, or already reviewed")
    })
    public HelpRequestResponse create(@PathVariable Long id,
                                      @Valid @RequestBody ReviewDto dto,
                                      Authentication authentication) {
        return reviewService.create(id, dto, authentication.getName());
    }
}