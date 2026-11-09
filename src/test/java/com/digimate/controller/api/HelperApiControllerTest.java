package com.digimate.controller.api;

import com.digimate.config.SecurityConfig;
import com.digimate.dto.HelpRequestResponse;
import com.digimate.entity.enums.RequestMode;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.service.HelperFlowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelperApiController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "hans@example.test")
class HelperApiControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean HelperFlowService helperFlowService;

    private HelpRequestResponse response(RequestStatus status) {
        return new HelpRequestResponse(5L, "Set up WhatsApp", "Help me install WhatsApp.", "Phone",
                LocalDateTime.of(2030, 11, 10, 15, 0), RequestMode.ONLINE, null, status,
                Instant.parse("2030-11-01T10:00:00Z"), null, null, null, null, null);
    }

    @Test
    void shouldListOpenRequests() throws Exception {
        when(helperFlowService.findOpen("hans@example.test")).thenReturn(List.of(response(RequestStatus.OPEN)));

        mockMvc.perform(get("/api/requests/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void shouldListTheRequestsIAccepted() throws Exception {
        when(helperFlowService.findHelping("hans@example.test")).thenReturn(List.of(response(RequestStatus.ACCEPTED)));

        mockMvc.perform(get("/api/requests/helping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"));
    }

    @Test
    void shouldAcceptARequestWithTheLoggedInUsername() throws Exception {
        when(helperFlowService.accept(5L, "hans@example.test")).thenReturn(response(RequestStatus.ACCEPTED));

        mockMvc.perform(post("/api/requests/5/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        verify(helperFlowService).accept(5L, "hans@example.test");
    }

    @Test
    void shouldCompleteARequest() throws Exception {
        when(helperFlowService.complete(5L, "hans@example.test")).thenReturn(response(RequestStatus.COMPLETED));

        mockMvc.perform(post("/api/requests/5/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}