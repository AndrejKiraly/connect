package com.andrejKir.connect.accounts.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class DuplicateUserException extends LocalizedException {

    public DuplicateUserException() {
        super("error.duplicate.user", "Email or username is already taken");
    }
}