package com.digimate.controller.api;


import com.digimate.dto.ComplaintDto;
import com.digimate.dto.ComplaintResponse;
import com.digimate.dto.DismissDto;
import com.digimate.entity.Complaint;
import com.digimate.service.ComplaintService;
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

/**
 * Report endpoints: a customer reports the helper of a request, and the admin lists
 * and closes the open reports. The controller only passes data to ComplaintService.
 * The security chain already requires ROLE_ADMIN for /api/admin/**.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Complaints", description = "Report a problem with a helper, and the admin's reports")
public class ComplaintApiController {


    private final ComplaintService complaintService;


    @PostMapping("/requests/{id}/complaint")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Report a problem with the helper of a request (only the creator, once)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "The report was saved"),
            @ApiResponse(responseCode = "400", description = "The reason is blank, too long, or has an ID or card number"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not the creator of the request"),
            @ApiResponse(responseCode = "404", description = "Request not found"),
            @ApiResponse(responseCode = "409", description = "No helper accepted the request yet, or it was already reported")
    })

    public ComplaintResponse create(@PathVariable Long id , @Valid @RequestBody ComplaintDto dto,
                                    Authentication authentication){

        return complaintService.create(id, dto, authentication.getName());
    }

    @GetMapping("/admin/complaints")
    @Operation(summary = "List the open reports (newest first)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The open reports"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    public List<ComplaintResponse> oneComplaints(){
        return complaintService.findOpen();
    }


    @PutMapping("/admin/complaints/{id}/dismiss")
    @Operation(summary = "Close a report without action (the note is optional)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The report is now DISMISSED"),
            @ApiResponse(responseCode = "400", description = "The note is too long or has an ID or card number"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not an admin"),
            @ApiResponse(responseCode = "404", description = "Report not found"),
            @ApiResponse(responseCode = "409", description = "The report is already closed")
    })
    public ComplaintResponse dismiss(@PathVariable Long id,
                                     @Valid @RequestBody(required = false) DismissDto dto) {
        return complaintService.dismiss(id, dto == null ? new DismissDto(null) : dto);
    }
}
