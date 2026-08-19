package com.andrejKir.connect.accounts.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class AppUserNotFoundException extends LocalizedException {

    public AppUserNotFoundException() {
        super("error.user.not-found", "User does not exist", (Object) null);
    }
}