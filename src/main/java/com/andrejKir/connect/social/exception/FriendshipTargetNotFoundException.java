package com.andrejKir.connect.social.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class FriendshipTargetNotFoundException extends LocalizedException {

    public FriendshipTargetNotFoundException() {
        super("error.friendship.target.not-found",
                "Friend request target does not exist", (Object) null);
    }
}