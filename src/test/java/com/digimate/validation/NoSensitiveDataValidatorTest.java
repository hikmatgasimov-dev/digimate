package com.digimate.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoSensitiveDataValidatorTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private record TestMessage(
            @NoSensitiveData String text
    ) {}

    private boolean isValid(String text) {
        return validator.validate(new TestMessage(text)).isEmpty();
    }

    @Test
    void acceptsNormalText() {
        assertTrue(isValid("Tuesday 15:00"));
        assertTrue(isValid("I need help with my laptop"));
    }

    @Test
    void acceptsNull() {
        assertTrue(isValid(null));
    }

    @Test
    void rejectsIdNumbers() {
        assertFalse(isValid("My ID is 123456-12345"));
    }

    @Test
    void rejectsCardNumbers() {
        assertFalse(isValid("4111111111111111"));
        assertFalse(isValid("4111 1111 1111 1111"));
        assertFalse(isValid("4111-1111-1111-1111"));
    }

    @Test
    void acceptsShortNumbers() {
        assertTrue(isValid("Call me at 12345"));
    }

    @Test
    void checksCardNumberLengthBoundaries() {
        assertFalse(isValid("1234567890123"));       // 13 digits
        assertFalse(isValid("1234567890123456789")); // 19 digits
        assertTrue(isValid("123456789012"));         // 12 digits
    }

}
