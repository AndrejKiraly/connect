package com.andrejKir.connect.messaging.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

public class SelfConversationException extends LocalizedException {

    public SelfConversationException() {
        super("error.conversation.self", "Cannot open a conversation with yourself", (Object) null);
    }
}