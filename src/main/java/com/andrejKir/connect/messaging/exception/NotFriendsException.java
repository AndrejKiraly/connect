package com.andrejKir.connect.messaging.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class NotFriendsException extends LocalizedException {

    public NotFriendsException() {
        super("error.conversation.not-friends", "Users are no longer friends", (Object) null);
    }
}