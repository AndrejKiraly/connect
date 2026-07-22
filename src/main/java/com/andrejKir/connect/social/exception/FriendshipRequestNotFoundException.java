package com.andrejKir.connect.social.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class FriendshipRequestNotFoundException extends LocalizedException {

    public FriendshipRequestNotFoundException() {
        super("error.friendship.request.not-found",
                "Friend request not found", (Object) null);
    }
}