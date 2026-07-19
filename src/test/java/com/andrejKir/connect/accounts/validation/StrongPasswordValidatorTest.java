package com.andrejKir.connect.accounts.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StrongPasswordValidatorTest {

    private static final String LENGTH_MESSAGE = "{com.andrejKir.connect.accounts.validation.StrongPassword.length}";
    private static final String COMMON_MESSAGE = "{com.andrejKir.connect.accounts.validation.StrongPassword.common}";

    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);
    private final StrongPasswordValidator validator = new StrongPasswordValidator(new WeakPasswordBlocklist());

    @Test
    void isValid_null_returnsTrue() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void isValid_sevenCharacters_returnsFalse() {
        assertFalse(validator.isValid(passwordOfLength(7), context));
        verify(context).buildConstraintViolationWithTemplate(LENGTH_MESSAGE);
    }

    @Test
    void isValid_eightCharacters_returnsTrue() {
        assertTrue(validator.isValid(passwordOfLength(8), context));
    }

    @Test
    void isValid_seventyTwoCharacters_returnsTrue() {
        assertTrue(validator.isValid(passwordOfLength(72), context));
    }

    @Test
    void isValid_seventyThreeCharacters_returnsFalse() {
        assertFalse(validator.isValid(passwordOfLength(73), context));
    }

    @Test
    void isValid_commonPassword_returnsFalse() {
        assertFalse(validator.isValid("password123", context));
        verify(context).buildConstraintViolationWithTemplate(COMMON_MESSAGE);
    }

    private static String passwordOfLength(int length) {
        String pattern = "Qx7!aB9-";
        return pattern.repeat(length / pattern.length() + 1).substring(0, length);
    }
}