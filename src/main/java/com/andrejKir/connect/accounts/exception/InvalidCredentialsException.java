package com.andrejKir.connect.accounts.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class InvalidCredentialsException extends LocalizedException {

    public InvalidCredentialsException() {
        super("error.invalid.credentials", "Username or password is incorrect ", (Object) null);
    }
}
