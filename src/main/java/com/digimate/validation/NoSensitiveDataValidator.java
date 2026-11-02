package com.digimate.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class NoSensitiveDataValidator
        implements ConstraintValidator<NoSensitiveData, String> {

    private static final Pattern ID_PATTERN =
            Pattern.compile("\\b\\d{6}-\\d{5}\\b");

    private static final Pattern CARD_PATTERN =
            Pattern.compile(
                    "(?<!\\d)(?<!\\d[ -])(?:\\d[ -]?){12}\\d" +
                            "(?:[ -]?\\d){0,6}(?![ -]?\\d)"
            );

    @Override
    public boolean isValid(
            String value,
            ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        return !ID_PATTERN.matcher(value).find()
                && !CARD_PATTERN.matcher(value).find();
    }
}
