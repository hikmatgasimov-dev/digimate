package com.digimate.controller.api;


/**
 * Help requests of the logged-in customer: create, read, update and cancel.
 * The controller only translates HTTP. All rules live in HelpRequestService.
 * The username (the email) comes from the login and is passed to the service as a string.
 */

import com.digimate.dto.HelpRequestDto;
import com.digimate.dto.HelpRequestResponse;
import com.digimate.entity.HelpRequest;
import com.digimate.service.HelpRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor


@Tag(name = "Requests", description = "Create, read, update and cancel help requests")
public class HelpRequestApiController {


    private final HelpRequestService helpRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a help request (status OPEN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "The request was created"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "403", description = "Admin accounts cannot ask for help"),
    })
    public HelpRequestResponse create(@Valid @RequestBody HelpRequestDto dto, Authentication authentication){
          return helpRequestService.create(dto,authentication.getName());
    }
    // "/my" is matched before "/{id}", so the two do not clash.
    @GetMapping("/my")
    @Operation(summary = "List my requests, newest first")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "My requests"),
            @ApiResponse(responseCode = "401", description = "Not logged in")
    })
    public List<HelpRequestResponse> findMine(Authentication authentication){
        return helpRequestService.findMine(authentication.getName());
    }
    @GetMapping("/{id}")
    @Operation(summary = "Get one request (creator, assigned helper or admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The request"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not allowed to see this request"),
            @ApiResponse(responseCode = "404", description = "Request not found")
    })
    public HelpRequestResponse findById(@PathVariable Long id, Authentication authentication){
        return helpRequestService.findById(id,authentication.getName());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Change a request (creator only, while OPEN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The request was changed"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not the creator"),
            @ApiResponse(responseCode = "404", description = "Request not found"),
            @ApiResponse(responseCode = "409", description = "The request is no longer OPEN")
    })
    public HelpRequestResponse update(@PathVariable Long id, @Valid @RequestBody HelpRequestDto dto,Authentication authentication ){
        return helpRequestService.update(id,dto,authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel a request (status CANCELLED, the record stays)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "The request was cancelled"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not the creator"),
            @ApiResponse(responseCode = "404", description = "Request not found"),
            @ApiResponse(responseCode = "409", description = "The request is no longer OPEN")
    })
    public void cancel(@PathVariable Long id,Authentication authentication ){
        helpRequestService.cancel(id,authentication.getName());
    }
}
