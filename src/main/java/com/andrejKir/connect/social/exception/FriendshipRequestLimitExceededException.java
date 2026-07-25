package com.andrejKir.connect.social.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class FriendshipRequestLimitExceededException extends LocalizedException {

    public FriendshipRequestLimitExceededException() {
        super("error.friendship.request.pending-limit", "Too many pending outgoing friend requests", (Object) null);
    }
}