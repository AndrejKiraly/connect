package com.andrejKir.connect.accounts.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {
    int MIN_LENGTH = 8;
    int MAX_LENGTH = 128;

    int min() default MIN_LENGTH;
    int max() default MAX_LENGTH;
    String message() default "{com.andrejKir.connect.accounts.validation.StrongPassword.message}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
