package com.digimate.controller.api;

import com.digimate.dto.HelpRequestResponse;
import com.digimate.service.HelperFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Helper endpoints: find open requests, accept one, complete it.
 * The controller only translates HTTP. All rules live in HelperFlowService.
 */
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Tag(name = "Helper", description = "Find, accept and complete help requests (verified helpers)")
public class HelperApiController {

    private final HelperFlowService helperFlowService;
    // "/helping" and "/open" are fixed paths, so they are matched before "/{id}" of the other controller.
    @GetMapping("/helping")
    @Operation(summary = "Requests I have accepted, newest first")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "My accepted requests"),
            @ApiResponse(responseCode = "401", description = "Not logged in")
    })
    public List<HelpRequestResponse> findHelping(Authentication authentication){
       return helperFlowService.findHelping(authentication.getName());
    }
    @GetMapping("/open")
    @Operation(summary = "Open requests of other people (verified helpers only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Open requests"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not a verified helper")
    })
    public List<HelpRequestResponse> findOpen(Authentication authentication) {
        return helperFlowService.findOpen(authentication.getName());
    }
    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept an open request (verified helpers only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The request was accepted"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not a verified helper, or your own request"),
            @ApiResponse(responseCode = "404", description = "Request not found"),
            @ApiResponse(responseCode = "409", description = "The request is no longer open")
    })
    public HelpRequestResponse accept(@PathVariable Long id,Authentication authentication){
        return helperFlowService.accept(id,authentication.getName());
    }
    @PostMapping("/{id}/complete")
    @Operation(summary = "Mark an accepted request as completed (assigned helper only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The request was completed"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not the assigned helper"),
            @ApiResponse(responseCode = "404", description = "Request not found"),
            @ApiResponse(responseCode = "409", description = "The request is not in status ACCEPTED")
    })
    public HelpRequestResponse complete(@PathVariable Long id, Authentication authentication) {
        return helperFlowService.complete(id, authentication.getName());
    }

}
