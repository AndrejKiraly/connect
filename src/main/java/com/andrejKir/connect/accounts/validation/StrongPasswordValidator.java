package com.andrejKir.connect.accounts.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;



public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 72;

    private final WeakPasswordBlocklist blocklist;

    public StrongPasswordValidator(WeakPasswordBlocklist blocklist) {
        this.blocklist = blocklist;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;

        if (value.length() < MIN_LENGTH || value.length()> MAX_LENGTH) return fail(context, "{com.andrejKir.connect.accounts.validation.StrongPassword.length}");

        if (blocklist.contains(value)) return fail(context, "{com.andrejKir.connect.accounts.validation.StrongPassword.common}");

        return true;
    }

    private boolean fail(ConstraintValidatorContext context, String messageTemplate) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(messageTemplate).addConstraintViolation();
        return false;
    }
}
