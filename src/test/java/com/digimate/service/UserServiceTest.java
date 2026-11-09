package com.digimate.service;

import com.digimate.dto.RegisterRequest;
import com.digimate.dto.UserResponse;
import com.digimate.entity.User;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.Role;
import com.digimate.exception.EmailAlreadyUsedException;
import com.digimate.exception.InvalidStatusTransitionException;
import com.digimate.exception.RequestNotFoundException;
import com.digimate.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for registration rules, helper verification and pending-helper mapping.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 17L;
    private static final String EMAIL = "anna.novak@example.com";
    private static final String PASSWORD = "secure-password-123";
    private static final String ENCODED_PASSWORD = "synthetic-encoded-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRejectAlreadyUsedEmail() {
        RegisterRequest request = registration(EMAIL, false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(USER_ID, HelperStatus.NONE)));

        assertThrows(EmailAlreadyUsedException.class, () -> userService.register(request));

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldRejectAlreadyUsedEmailWithDifferentCase() {
        RegisterRequest request = registration("Anna.Novak@EXAMPLE.COM", false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(USER_ID, HelperStatus.NONE)));

        assertThrows(EmailAlreadyUsedException.class, () -> userService.register(request));

        verify(userRepository).findByEmail(EMAIL);
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldSaveEncodedPassword() {
        RegisterRequest request = registration(EMAIL, false);
        prepareSuccessfulRegistration();

        userService.register(request);

        assertThat(capturedUser().getPasswordHash()).isEqualTo(ENCODED_PASSWORD).isNotEqualTo(PASSWORD);
        verify(passwordEncoder).encode(PASSWORD);
    }

    @Test
    void shouldThrowRequestNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> userService.verifyHelper(USER_ID));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectVerificationWhenHelperStatusIsNone() {
        User user = user(USER_ID, HelperStatus.NONE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThrows(InvalidStatusTransitionException.class, () -> userService.verifyHelper(USER_ID));

        assertThat(user.getHelperStatus()).isEqualTo(HelperStatus.NONE);
        assertThat(user.getRole()).isEqualTo(Role.USER);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectVerificationWhenHelperIsAlreadyVerified() {
        User user = user(USER_ID, HelperStatus.VERIFIED);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThrows(InvalidStatusTransitionException.class, () -> userService.verifyHelper(USER_ID));

        assertThat(user.getHelperStatus()).isEqualTo(HelperStatus.VERIFIED);
        assertThat(user.getRole()).isEqualTo(Role.USER);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldNormalizeEmailBeforeSaving() {
        RegisterRequest request = registration("  Anna.Novak@EXAMPLE.COM  ", false);
        prepareSuccessfulRegistration();

        UserResponse response = userService.register(request);

        verify(userRepository).findByEmail(EMAIL);
        assertThat(capturedUser().getEmail()).isEqualTo(EMAIL);
        assertThat(response.email()).isEqualTo(EMAIL);
    }

    @Test
    void shouldAssignUserRoleOnRegistration() {
        RegisterRequest request = registration(EMAIL, false);
        prepareSuccessfulRegistration();

        UserResponse response = userService.register(request);

        assertThat(capturedUser().getRole()).isEqualTo(Role.USER);
        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    void shouldRegisterCustomerWithNoHelperStatus() {
        RegisterRequest request = registration(EMAIL, false);
        prepareSuccessfulRegistration();

        UserResponse response = userService.register(request);

        assertThat(capturedUser().getHelperStatus()).isEqualTo(HelperStatus.NONE);
        assertThat(response.helperStatus()).isEqualTo(HelperStatus.NONE);
    }

    @Test
    void shouldRegisterHelperAsPending() {
        RegisterRequest request = registration(EMAIL, true);
        prepareSuccessfulRegistration();

        UserResponse response = userService.register(request);

        assertThat(capturedUser().getHelperStatus()).isEqualTo(HelperStatus.PENDING);
        assertThat(response.helperStatus()).isEqualTo(HelperStatus.PENDING);
    }

    @Test
    void shouldVerifyPendingHelper() {
        User pendingHelper = user(USER_ID, HelperStatus.PENDING);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(pendingHelper));

        UserResponse response = userService.verifyHelper(USER_ID);

        assertThat(pendingHelper.getHelperStatus()).isEqualTo(HelperStatus.VERIFIED);
        assertThat(response).isEqualTo(new UserResponse(
                USER_ID, pendingHelper.getFullName(), EMAIL, Role.USER, HelperStatus.VERIFIED));
    }

    @Test
    void shouldReturnPendingHelpers() {
        User anna = user(USER_ID, HelperStatus.PENDING);
        User markus = user(18L, HelperStatus.PENDING);
        markus.setFullName("Markus Berg");
        markus.setEmail("markus.berg@example.com");
        when(userRepository.findByHelperStatus(HelperStatus.PENDING)).thenReturn(List.of(anna, markus));

        List<UserResponse> response = userService.findPendingHelpers();

        assertThat(response).containsExactly(
                new UserResponse(USER_ID, "Anna Novak", EMAIL, Role.USER, HelperStatus.PENDING),
                new UserResponse(18L, "Markus Berg", "markus.berg@example.com", Role.USER, HelperStatus.PENDING));
    }

    @Test
    void shouldReturnEmptyListWhenNoHelpersArePending() {
        when(userRepository.findByHelperStatus(HelperStatus.PENDING)).thenReturn(List.of());

        List<UserResponse> response = userService.findPendingHelpers();

        assertThat(response).isEmpty();
    }

    private void prepareSuccessfulRegistration() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(USER_ID);
            return saved;
        });
    }

    private User capturedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    private static RegisterRequest registration(String email, boolean wantsToHelp) {
        return new RegisterRequest("Anna Novak", email, "+37120000001", PASSWORD, wantsToHelp);
    }

    private static User user(Long id, HelperStatus status) {
        User user = new User();
        user.setId(id);
        user.setFullName("Anna Novak");
        user.setEmail(EMAIL);
        user.setPhone("+37120000001");
        user.setPasswordHash(ENCODED_PASSWORD);
        user.setRole(Role.USER);
        user.setHelperStatus(status);
        return user;
    }
}
