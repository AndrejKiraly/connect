package com.andrejKir.connect.social.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class AlreadyFriendsException extends LocalizedException {

    public AlreadyFriendsException() {
        super("error.friendship.already-friends", "Users are already friends", (Object) null);
    }
}