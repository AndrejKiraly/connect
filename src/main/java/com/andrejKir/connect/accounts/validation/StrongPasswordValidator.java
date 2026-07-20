package com.andrejKir.connect.accounts.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;



public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private final WeakPasswordBlocklist blocklist;

    private int minLength;
    private int maxLength;

    public StrongPasswordValidator(WeakPasswordBlocklist blocklist) {
        this.blocklist = blocklist;
    }

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.minLength = constraintAnnotation.min();
        this.maxLength = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;

        if (value.length() < minLength || value.length()> maxLength) return fail(context, "{com.andrejKir.connect.accounts.validation.StrongPassword.length}");

        if (blocklist.contains(value)) return fail(context, "{com.andrejKir.connect.accounts.validation.StrongPassword.common}");

        return true;
    }

    private boolean fail(ConstraintValidatorContext context, String messageTemplate) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(messageTemplate).addConstraintViolation();
        return false;
    }
}
