package com.andrejKir.connect.accounts.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class InvalidPasswordResetTokenException extends LocalizedException {

    public InvalidPasswordResetTokenException() {
        super("error.password-reset.invalid-token",
                "Password reset token is invalid, used or expired", (Object) null);
    }
}