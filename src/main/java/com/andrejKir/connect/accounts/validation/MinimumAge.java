package com.andrejKir.connect.accounts.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinimumAgeValidator.class)
public @interface MinimumAge {
    int value();
    String message() default "{com.andrejKir.connect.accounts.validation.MinimumAge.message}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};


}
