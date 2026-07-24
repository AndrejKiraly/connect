package com.andrejKir.connect.social.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class InvalidFriendshipDirectionException extends LocalizedException {

    public InvalidFriendshipDirectionException() {
        super("error.friendship.direction.invalid", "Friend request direction must be incoming or outgoing", (Object) null);
    }
}