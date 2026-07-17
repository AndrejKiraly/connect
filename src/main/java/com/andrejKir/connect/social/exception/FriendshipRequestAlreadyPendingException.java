package com.andrejKir.connect.social.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class FriendshipRequestAlreadyPendingException extends LocalizedException {

    public FriendshipRequestAlreadyPendingException (){
        super ("error.friendship.request.already-pending", "Friend request is already pending", (Object) null);
    }
}
