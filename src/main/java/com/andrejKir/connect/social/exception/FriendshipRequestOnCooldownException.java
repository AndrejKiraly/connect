package com.andrejKir.connect.social.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class FriendshipRequestOnCooldownException extends LocalizedException {

    public FriendshipRequestOnCooldownException() {
        super("error.friendship.request.on-cooldown",
                "Cannot send another friend request yet", (Object) null);
    }
}