package com.digimate.controller.api;

import com.digimate.config.SecurityConfig;
import com.digimate.dto.RegisterRequest;
import com.digimate.exception.EmailAlreadyUsedException;
import com.digimate.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;

/**
 * MVC tests for public registration validation and duplicate-email error handling.
 */
@WebMvcTest(AuthApiController.class)
@Import(SecurityConfig.class)
class AuthApiControllerTest {

    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String VALID_EMAIL = "anna.novak@example.com";
    private static final String VALID_PASSWORD = "secure-password-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        RegisterRequest request = registration("not-an-email", VALID_PASSWORD);

        ResultActions result = register(request);

        expectValidationError(result, "email");
        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn400WhenPasswordIsTooShort() throws Exception {
        RegisterRequest request = registration(VALID_EMAIL, "a".repeat(7));

        ResultActions result = register(request);

        expectValidationError(result, "password");
        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn400WhenPasswordIsTooLong() throws Exception {
        RegisterRequest request = registration(VALID_EMAIL, "a".repeat(73));

        ResultActions result = register(request);

        expectValidationError(result, "password");
        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn409WhenEmailIsAlreadyUsed() throws Exception {
        RegisterRequest request = registration(VALID_EMAIL, VALID_PASSWORD);
        String message = "This email is already registered.";
        when(userService.register(request)).thenThrow(new EmailAlreadyUsedException(message));

        ResultActions result = register(request);

        expectError(result, 409);
        result.andExpect(jsonPath("$.message").value(message));
        verify(userService).register(request);
    }

    @Test
    void shouldReturn400WhenRequestBodyIsMissing() throws Exception {
        ResultActions result = mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON));

        expectUnreadableRequest(result);
        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        String json = "{\"fullName\": \"Anna Novak\",";

        ResultActions result = mockMvc.perform(post(REGISTER_PATH)
                .contentType(MediaType.APPLICATION_JSON).content(json));

        expectUnreadableRequest(result);
        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn400WhenHelperPreferenceHasWrongType() throws Exception {
        String json = """
                {
                  "fullName": "Anna Novak",
                  "email": "anna.novak@example.com",
                  "phone": "+37120000001",
                  "password": "secure-password-123",
                  "wantsToHelp": {}
                }
                """;

        ResultActions result = mockMvc.perform(post(REGISTER_PATH)
                .contentType(MediaType.APPLICATION_JSON).content(json));

        expectUnreadableRequest(result);
        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn400WhenFullNameIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "   ", VALID_EMAIL, "+37120000001", VALID_PASSWORD, false);

        ResultActions result = register(request);

        expectValidationError(result, "fullName");
        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn400WhenPhoneIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Anna Novak", VALID_EMAIL, "   ", VALID_PASSWORD, false);

        ResultActions result = register(request);

        expectValidationError(result, "phone");
        verifyNoInteractions(userService);
    }

    private ResultActions register(RegisterRequest request) throws Exception {
        return mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));
    }

    private static void expectValidationError(ResultActions result, String field) throws Exception {
        expectError(result, 400);
        result.andExpect(jsonPath("$.message", containsString(field + ":")));
    }

    private static void expectUnreadableRequest(ResultActions result) throws Exception {
        expectError(result, 400);
        result.andExpect(jsonPath("$.message")
                .value("The request could not be read. Please check the values."));
    }

    private static void expectError(ResultActions result, int expectedStatus) throws Exception {
        result.andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.path").value(REGISTER_PATH))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    private static RegisterRequest registration(String email, String password) {
        return new RegisterRequest("Anna Novak", email, "+37120000001", password, false);
    }
    @Test
    void shouldRegisterWhenTheHelperPreferenceIsMissing() throws Exception {
        String json = """
                {"fullName":"Anna Novak","email":"anna.novak@example.com","phone":"+37120000001","password":"secure-password-123"}
                """;

        mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());

        verify(userService).register(any(RegisterRequest.class));
    }
}
