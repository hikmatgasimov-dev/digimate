package com.digimate.dto;

import com.digimate.entity.enums.RequestMode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelpRequestDtoTest {

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

    private HelpRequestDto withDescription(String description) {
        return new HelpRequestDto(
                "Set up WhatsApp",
                description,
                1L,
                LocalDateTime.now().plusDays(2),
                RequestMode.IN_PERSON,
                "City library");
    }

    @Test
    void acceptsNormalDescription() {
        assertTrue(validator.validate(withDescription("Help me install WhatsApp.")).isEmpty());
    }

    @Test
    void rejectsCardNumberInDescription() {
        Set<ConstraintViolation<HelpRequestDto>> violations =
                validator.validate(withDescription("My card is 4111 1111 1111 1111"));

        assertEquals(1, violations.size());
        assertEquals("description", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void rejectsIdNumberInDescription() {
        Set<ConstraintViolation<HelpRequestDto>> violations =
                validator.validate(withDescription("My ID is 123456-12345"));

        assertEquals(1, violations.size());
        assertEquals("description", violations.iterator().next().getPropertyPath().toString());
    }
}