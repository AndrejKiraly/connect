package com.andrejKir.connect.accounts.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class DuplicateUsernameException extends LocalizedException {

    public DuplicateUsernameException(String username) {
        super("error.duplicate.username", "Username already taken: " + username, username);
    }
}