package com.andrejKir.connect.social.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class FriendshipNotPendingException extends LocalizedException {

    public FriendshipNotPendingException() {
        super("error.friendship.not-pending",
                "Friend request is no longer pending", (Object) null);
    }
}