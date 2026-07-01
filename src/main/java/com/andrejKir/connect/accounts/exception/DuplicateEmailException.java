package com.andrejKir.connect.accounts.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class DuplicateEmailException extends LocalizedException {

    public DuplicateEmailException(String email) {
        super("error.duplicate.email", "Email already registered: " + email, email);
    }
}