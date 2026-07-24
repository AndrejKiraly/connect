package com.andrejKir.connect.social.enums;

import com.andrejKir.connect.social.exception.InvalidFriendshipDirectionException;

public enum RequestDirection {
    INCOMING,
    OUTGOING;

    public static RequestDirection from(String value) {
        for (RequestDirection direction : values()) {
            if (direction.name().equalsIgnoreCase(value)) {
                return direction;
            }
        }
        throw new InvalidFriendshipDirectionException();
    }
}