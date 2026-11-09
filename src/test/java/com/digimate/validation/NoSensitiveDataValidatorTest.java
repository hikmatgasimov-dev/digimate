package com.digimate.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoSensitiveDataValidatorTest {

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

    private record TestMessage(
            @NoSensitiveData String text
    ) {}

    private boolean isValid(String text) {
        return validator.validate(new TestMessage(text)).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "Tuesday 15:00",
            "I need help with my laptop",
            "Please show me how to send an email.",
            "Call me at 12345",
            "123456789012" // Below the 13-digit card-number boundary.
    })
    void shouldAcceptTextWithoutSensitiveNumbers(String text) {
        assertTrue(isValid(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "My ID is 123456-12345",
            "4111111111111111",
            "4111 1111 1111 1111",
            "4111-1111-1111-1111",
            "1234567890123", // 13-digit lower boundary.
            "1234567890123456789" // 19-digit upper boundary.
    })
    void shouldRejectTextWithSensitiveNumbers(String text) {
        assertFalse(isValid(text));
    }

}