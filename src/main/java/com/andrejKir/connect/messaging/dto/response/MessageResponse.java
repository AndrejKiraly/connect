package com.andrejKir.connect.messaging.dto.response;

import com.andrejKir.connect.messaging.entity.Message;
import com.andrejKir.connect.messaging.enums.MessageType;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        MessageType type,
        String body,
        Instant createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getType(),
                message.getBody(),
                message.getCreatedAt());
    }
}