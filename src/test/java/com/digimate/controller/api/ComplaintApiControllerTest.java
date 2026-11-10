package com.digimate.controller.api;
import com.digimate.config.SecurityConfig;
import com.digimate.dto.ComplaintDto;
import com.digimate.dto.ComplaintResponse;
import com.digimate.dto.DismissDto;
import com.digimate.entity.enums.ComplaintStatus;
import com.digimate.service.ComplaintService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComplaintApiController.class)
@Import(SecurityConfig.class)
class ComplaintApiControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ComplaintService complaintService;
    @MockitoBean JwtDecoder jwtDecoder;

    private ComplaintResponse response(ComplaintStatus status) {
        return new ComplaintResponse(7L, 5L, 2L, "Helper One", "helper@example.test", "Anna",
                "The helper did not show up.", status, Instant.parse("2030-11-01T10:00:00Z"));
    }

    @Test
    @WithMockUser(username = "anna@example.test")
    void shouldSaveAReportAndReturn201WithTheLoggedInUsername() throws Exception {
        when(complaintService.create(eq(5L), any(ComplaintDto.class), eq("anna@example.test")))
                .thenReturn(response(ComplaintStatus.OPEN));

        mockMvc.perform(post("/api/requests/5/complaint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"The helper did not show up.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.requestId").value(5))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(complaintService).create(5L, new ComplaintDto("The helper did not show up."), "anna@example.test");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldListTheOpenReportsForTheAdmin() throws Exception {
        when(complaintService.findOpen()).thenReturn(List.of(response(ComplaintStatus.OPEN)));

        mockMvc.perform(get("/api/admin/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].helperName").value("Helper One"))
                .andExpect(jsonPath("$[0].customerFirstName").value("Anna"))
                .andExpect(jsonPath("$[0].helperPhone").doesNotExist())
                .andExpect(jsonPath("$[0].customerPhone").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDismissAReportWithANote() throws Exception {
        when(complaintService.dismiss(eq(7L), any(DismissDto.class))).thenReturn(response(ComplaintStatus.DISMISSED));

        mockMvc.perform(put("/api/admin/complaints/7/dismiss")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Misunderstanding\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));

        verify(complaintService).dismiss(7L, new DismissDto("Misunderstanding"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDismissAReportWithoutABody() throws Exception {
        when(complaintService.dismiss(eq(7L), any(DismissDto.class))).thenReturn(response(ComplaintStatus.DISMISSED));

        mockMvc.perform(put("/api/admin/complaints/7/dismiss"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));

        verify(complaintService).dismiss(7L, new DismissDto(null));
    }
}