package com.andrejKir.connect.messaging.exception;

import com.andrejKir.connect.shared.exception.LocalizedException;

import java.util.UUID;

public class ConversationNotFoundException extends LocalizedException {

    public ConversationNotFoundException(UUID conversationId) {
        super("error.conversation.not-found", "Conversation not found or not accessible: " + conversationId, (Object) null);
    }
}