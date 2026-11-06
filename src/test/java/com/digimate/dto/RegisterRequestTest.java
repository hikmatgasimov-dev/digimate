package com.digimate.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for RegisterRequest validation using Bean Validation,
 * without starting a Spring context or connecting to a database.
 */

class RegisterRequestTest {

    private static final String VALID_FULL_NAME = "Test User";
    private static final String VALID_EMAIL = "user@example.test";
    private static final String VALID_PHONE = "20123456";
    private static final String VALID_PASSWORD = "test-password";

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectMissingOrBlankFullName(String fullName) {
        RegisterRequest request = new RegisterRequest(
                fullName, VALID_EMAIL, VALID_PHONE, VALID_PASSWORD, false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertRequiredFieldViolation(violations, "fullName");
    }

    @Test
    void shouldAcceptFullNameAtMaximumLength() {
        RegisterRequest request = new RegisterRequest(
                "a".repeat(100), VALID_EMAIL, VALID_PHONE, VALID_PASSWORD, false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectFullNameOverMaximumLength() {
        RegisterRequest request = new RegisterRequest(
                "a".repeat(101), VALID_EMAIL, VALID_PHONE, VALID_PASSWORD, false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertSingleViolation(violations, "fullName", Size.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectMissingOrBlankEmail(String email) {
        RegisterRequest request = new RegisterRequest(
                VALID_FULL_NAME, email, VALID_PHONE, VALID_PASSWORD, false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertRequiredFieldViolation(violations, "email");
    }

    @Test
    void shouldAcceptValidEmail() {
        RegisterRequest request = new RegisterRequest(
                VALID_FULL_NAME, VALID_EMAIL, VALID_PHONE, VALID_PASSWORD, false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectInvalidEmail() {
        RegisterRequest request = new RegisterRequest(
                VALID_FULL_NAME, "not-an-email", VALID_PHONE, VALID_PASSWORD, false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertSingleViolation(violations, "email", Email.class);
    }

    @Test
    void shouldAcceptEmailAtMaximumLength() {
        String email = "a".repeat(64) + "@"
                + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(57) + ".test";
        RegisterRequest request = new RegisterRequest(
                VALID_FULL_NAME, email, VALID_PHONE, VALID_PASSWORD, false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertEquals(255, email.length());
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectEmailOverMaximumLength() {
        String email = "a".repeat(64) + "@"
                + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(58) + ".test";
        RegisterRequest request = new RegisterRequest(
                VALID_FULL_NAME, email, VALID_PHONE, VALID_PASSWORD, false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertEquals(256, email.length());
        assertSingleViolation(violations, "email", Size.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectMissingOrBlankPhone(String phone) {
        RegisterRequest request = new RegisterRequest(
                VALID_FULL_NAME, VALID_EMAIL, phone, VALID_PASSWORD, false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertRequiredFieldViolation(violations, "phone");
    }

    @Test
    void shouldAcceptPhoneAtMaximumLength() {
        RegisterRequest request = new RegisterRequest(
                VALID_FULL_NAME, VALID_EMAIL, "1".repeat(30), VALID_PASSWORD, false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectPhoneOverMaximumLength() {
        RegisterRequest request = new RegisterRequest(
                VALID_FULL_NAME, VALID_EMAIL, "1".repeat(31), VALID_PASSWORD, false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertSingleViolation(violations, "phone", Size.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectMissingOrBlankPassword(String password) {
        RegisterRequest request = new RegisterRequest(
                VALID_FULL_NAME, VALID_EMAIL, VALID_PHONE, password, false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertRequiredFieldViolation(violations, "password");
    }

    @Test
    void shouldRejectPasswordBelowMinimumLength() {
        RegisterRequest request = new RegisterRequest(
                VALID_FULL_NAME, VALID_EMAIL, VALID_PHONE, "a".repeat(7), false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertSingleViolation(violations, "password", Size.class);
    }

    @Test
    void shouldAcceptPasswordAtMinimumLength() {
        RegisterRequest request = new RegisterRequest(
                VALID_FULL_NAME, VALID_EMAIL, VALID_PHONE, "a".repeat(8), false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldAcceptPasswordAtMaximumLength() {
        RegisterRequest request = new RegisterRequest(
                VALID_FULL_NAME, VALID_EMAIL, VALID_PHONE, "a".repeat(72), false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectPasswordOverMaximumLength() {
        RegisterRequest request = new RegisterRequest(
                VALID_FULL_NAME, VALID_EMAIL, VALID_PHONE, "a".repeat(73), false);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertSingleViolation(violations, "password", Size.class);
    }

    private static void assertRequiredFieldViolation(
            Set<ConstraintViolation<RegisterRequest>> violations, String field) {
        assertEquals(Set.of(field), violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet()));
        assertTrue(violations.stream().anyMatch(violation ->
                violation.getConstraintDescriptor().getAnnotation().annotationType() == NotBlank.class));
    }

    private static void assertSingleViolation(
            Set<ConstraintViolation<RegisterRequest>> violations,
            String field,
            Class<? extends Annotation> constraint) {
        assertEquals(1, violations.size());
        ConstraintViolation<RegisterRequest> violation = violations.iterator().next();
        assertEquals(field, violation.getPropertyPath().toString());
        assertEquals(constraint, violation.getConstraintDescriptor().getAnnotation().annotationType());
    }
}
