package com.digimate.controller.api;

import com.digimate.config.SecurityConfig;
import com.digimate.dto.HelpRequestDto;
import com.digimate.dto.HelpRequestResponse;
import com.digimate.entity.enums.RequestMode;
import com.digimate.entity.enums.RequestStatus;
import com.digimate.service.HelpRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelpRequestApiController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "anna@example.test")
class HelpRequestApiControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean HelpRequestService helpRequestService;

    private static final String BODY = """
            {"title":"Set up WhatsApp","description":"Help me install WhatsApp.","categoryId":1,
             "preferredTime":"2030-11-10T15:00:00","mode":"IN_PERSON","place":"City library"}
            """;

    private HelpRequestResponse response(RequestStatus status) {
        return new HelpRequestResponse(5L, "Set up WhatsApp", "Help me install WhatsApp.", "Phone",
                LocalDateTime.of(2030, 11, 10, 15, 0), RequestMode.IN_PERSON, "City library", status,
                Instant.parse("2030-11-01T10:00:00Z"), null, null, null, null, null);
    }

    @Test
    void shouldCreateARequestAndReturn201WithTheLoggedInUsername() throws Exception {
        when(helpRequestService.create(any(HelpRequestDto.class), eq("anna@example.test")))
                .thenReturn(response(RequestStatus.OPEN));

        mockMvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void shouldListMyRequests() throws Exception {
        when(helpRequestService.findMine("anna@example.test")).thenReturn(List.of(response(RequestStatus.OPEN)));

        mockMvc.perform(get("/api/requests/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Set up WhatsApp"));
        // "/my" must not be treated as an id
        verify(helpRequestService, never()).findById(any(), any());
    }

    @Test
    void shouldGetOneRequest() throws Exception {
        when(helpRequestService.findById(5L, "anna@example.test")).thenReturn(response(RequestStatus.OPEN));

        mockMvc.perform(get("/api/requests/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void shouldUpdateARequest() throws Exception {
        when(helpRequestService.update(eq(5L), any(HelpRequestDto.class), eq("anna@example.test")))
                .thenReturn(response(RequestStatus.OPEN));

        mockMvc.perform(put("/api/requests/5").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.place").value("City library"));
    }

    @Test
    void shouldCancelARequestAndReturn204() throws Exception {
        mockMvc.perform(delete("/api/requests/5")).andExpect(status().isNoContent());

        verify(helpRequestService).cancel(5L, "anna@example.test");
    }
    @Test
    void shouldReturn400WhenTheTitleContainsACardNumber() throws Exception {
        String body = BODY.replace("Set up WhatsApp", "Pay with 4111 1111 1111 1111");

        mockMvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(helpRequestService, never()).create(any(), any());
    }

    @Test
    void shouldReturn400WhenThePlaceContainsACardNumber() throws Exception {
        String body = BODY.replace("City library", "Card 4111 1111 1111 1111");

        mockMvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(helpRequestService, never()).create(any(), any());
    }

    @Test
    void shouldReturn403WhenAnAdminTriesToCreateARequest() throws Exception {
        when(helpRequestService.create(any(HelpRequestDto.class), any()))
                .thenThrow(new AccessDeniedException("Admin accounts cannot ask for help"));

        mockMvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }
    @Test
    void shouldShowTheRealMessageWhenTheInPersonPlaceIsMissing() throws Exception {
        when(helpRequestService.create(any(HelpRequestDto.class), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Place is required for in-person help"));

        mockMvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Place is required for in-person help"));
    }
}