package com.digimate.exception;

import com.digimate.config.SecurityConfig;
import com.digimate.controller.api.AuthApiController;
import com.digimate.controller.api.HelpRequestApiController;
import com.digimate.controller.api.HelperApiController;
import com.digimate.controller.api.LoginApiController;
import com.digimate.dto.RegisterRequest;
import com.digimate.service.HelpRequestService;
import com.digimate.service.HelperFlowService;
import com.digimate.service.JwtService;
import com.digimate.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC tests for exception-handler selection and error status, message, path and timestamp.
 */
@WebMvcTest({AuthApiController.class, HelpRequestApiController.class,
        HelperApiController.class, LoginApiController.class})
@Import(SecurityConfig.class)
@WithMockUser(username = "viewer@example.test")
class GlobalExceptionHandlerTest {

    private static final String USERNAME = "viewer@example.test";
    private static final String PATH = "/api/requests/17";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String LOGIN_PATH = "/api/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private HelpRequestService helpRequestService;

    @MockitoBean
    private HelperFlowService helperFlowService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtService jwtService;

    private Instant startedAt;

    @BeforeEach
    void setUp() {
        startedAt = Instant.now();
    }

    @Test
    void shouldReturn404WhenResourceIsMissing() throws Exception {
        when(helpRequestService.findById(17L, USERNAME))
                .thenThrow(new RequestNotFoundException("Request not found"));

        ResultActions result = mockMvc.perform(get(PATH));

        assertError(result, 404, "Request not found", PATH);
    }

    @Test
    void shouldReturn409WhenRequestIsAlreadyTaken() throws Exception {
        String path = PATH + "/accept";
        when(helperFlowService.accept(17L, USERNAME))
                .thenThrow(new RequestAlreadyTakenException("This request is no longer open"));

        ResultActions result = mockMvc.perform(post(path));

        assertError(result, 409, "This request is no longer open", path);
    }

    @Test
    void shouldReturn409WhenStatusTransitionIsInvalid() throws Exception {
        when(helpRequestService.cancel(17L, USERNAME))
                .thenThrow(new InvalidStatusTransitionException("Only open requests can be changed"));

        ResultActions result = mockMvc.perform(delete(PATH));

        assertError(result, 409, "Only open requests can be changed", PATH);
    }

    @Test
    void shouldReturn409WhenReviewAlreadyExists() throws Exception {
        // No review endpoint exists yet; check the advice mapping through an existing API route.
        when(helpRequestService.findById(17L, USERNAME))
                .thenThrow(new DuplicateReviewException("A review already exists"));

        ResultActions result = mockMvc.perform(get(PATH));

        assertError(result, 409, "A review already exists", PATH);
    }

    @Test
    void shouldReturn409WhenEmailIsAlreadyUsed() throws Exception {
        RegisterRequest input = validRegistration();
        when(userService.register(input))
                .thenThrow(new EmailAlreadyUsedException("This email is already registered."));

        ResultActions result = register(input);

        assertError(result, 409, "This email is already registered.", REGISTER_PATH);
    }

    @Test
    void shouldReturn403WhenHelperIsNotVerified() throws Exception {
        String path = "/api/requests/open";
        when(helperFlowService.findOpen(USERNAME))
                .thenThrow(new HelperNotVerifiedException("Only verified helpers can do this"));

        ResultActions result = mockMvc.perform(get(path));

        assertError(result, 403, "Only verified helpers can do this", path);
    }

    @Test
    void shouldReturn403WithoutExposingInternalAccessDeniedMessage() throws Exception {
        when(helpRequestService.findById(17L, USERNAME))
                .thenThrow(new AccessDeniedException("Internal authorization detail"));

        ResultActions result = mockMvc.perform(get(PATH));

        assertError(result, 403, "You are not allowed to do this.", PATH);
    }

    @Test
    void shouldReturn400WithEachInvalidField() throws Exception {
        RegisterRequest input = new RegisterRequest("Anna Novak", "invalid", "+37120000001", "short", false);

        ResultActions result = register(input);

        assertErrorMetadata(result, 400, REGISTER_PATH);
        result.andExpect(jsonPath("$.message", containsString("email: must be a well-formed email address")))
                .andExpect(jsonPath("$.message", containsString("password: size must be between 8 and 72")));
        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn400WithoutExposingJsonParserDetails() throws Exception {
        ResultActions result = mockMvc.perform(post(REGISTER_PATH)
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":"));

        assertError(result, 400, "The request could not be read. Please check the values.", REGISTER_PATH);
        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn400WhenArgumentTypeDoesNotMatch() throws Exception {
        String path = "/api/requests/invalid-id";

        ResultActions result = mockMvc.perform(get(path));

        assertError(result, 400, "The request could not be read. Please check the values.", path);
        verifyNoInteractions(helpRequestService);
    }

    @Test
    @WithAnonymousUser
    void shouldReturn401WithoutDisclosingWhyAuthenticationFailed() throws Exception {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Internal authentication detail"));

        ResultActions result = mockMvc.perform(post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"viewer@example.test","password":"wrong-password"}
                        """));

        assertError(result, 401, "Invalid email or password.", LOGIN_PATH);
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldReturn409WhenOptimisticLockRejectsConcurrentChange() throws Exception {
        String path = PATH + "/accept";
        when(helperFlowService.accept(17L, USERNAME))
                .thenThrow(new ObjectOptimisticLockingFailureException("HelpRequest", 17L));

        ResultActions result = mockMvc.perform(post(path));

        assertError(result, 409,
                "This request was just changed by someone else. Please reload and try again.", path);
    }

    @Test
    void shouldReturn409WithoutExposingDatabaseConstraintDetails() throws Exception {
        RegisterRequest input = validRegistration();
        when(userService.register(input))
                .thenThrow(new DataIntegrityViolationException("Internal database detail"));

        ResultActions result = register(input);

        assertError(result, 409, "This change conflicts with existing data.", REGISTER_PATH);
    }

    @Test
    void shouldReturn500WithoutExposingUnexpectedExceptionDetails() throws Exception {
        when(helpRequestService.findById(17L, USERNAME))
                .thenThrow(new IllegalStateException("Internal implementation detail"));

        ResultActions result = mockMvc.perform(get(PATH));

        assertError(result, 500, "Something went wrong. Please try again later.", PATH);
    }

    @Test
    void shouldPreserveStatusOfSpringWebErrorWithoutReason() throws Exception {
        when(helpRequestService.findById(17L, USERNAME))
                .thenThrow(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED));

        ResultActions result = mockMvc.perform(get(PATH));

        assertError(result, 405, "Method Not Allowed", PATH);
    }

    private ResultActions register(RegisterRequest input) throws Exception {
        return mockMvc.perform(post(REGISTER_PATH).locale(Locale.ENGLISH)
                .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(input)));
    }

    private void assertError(ResultActions result, int expectedStatus, String message, String path) throws Exception {
        assertErrorMetadata(result, expectedStatus, path);
        result.andExpect(jsonPath("$.message").value(message));
    }

    private void assertErrorMetadata(ResultActions result, int expectedStatus, String path) throws Exception {
        result.andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(response -> {
                    ErrorResponse body = jsonMapper.readValue(response.getResponse().getContentAsString(),
                            ErrorResponse.class);
                    assertThat(body.timestamp()).isBetween(startedAt, Instant.now());
                });
    }

    private static RegisterRequest validRegistration() {
        return new RegisterRequest("Anna Novak", "anna.novak@example.test", "+37120000001", "secure-password", false);
    }
}
