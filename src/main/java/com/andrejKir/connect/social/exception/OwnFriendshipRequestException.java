package com.andrejKir.connect.social.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class OwnFriendshipRequestException extends LocalizedException {

    public OwnFriendshipRequestException() {
        super("error.friendship.request.own",
                "Cannot accept your own friend request", (Object) null);
    }
}