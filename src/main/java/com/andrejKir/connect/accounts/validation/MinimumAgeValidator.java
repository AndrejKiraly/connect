package com.andrejKir.connect.accounts.validation;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class MinimumAgeValidator implements ConstraintValidator<MinimumAge, LocalDate> {
    private int min_age;

    @Override
    public void initialize(MinimumAge age) {
       this.min_age = age.value();
    }

    @Override
    public boolean isValid(LocalDate birth_date, ConstraintValidatorContext context) {
        return birth_date == null || !birth_date.plusYears(min_age).isAfter(LocalDate.now());
    }
}
