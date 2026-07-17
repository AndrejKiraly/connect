package com.andrejKir.connect.social.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class SelfFriendshipException extends LocalizedException {

    public SelfFriendshipException() {
        super("error.friendship.self", "Cannot send a friend request to yourself", (Object) null);
    }
}
