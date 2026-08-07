package com.andrejKir.connect.messaging.dto.response;

import com.andrejKir.connect.messaging.entity.Message;
import com.andrejKir.connect.messaging.enums.MessageType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        MessageType type,
        String body,
        Instant createdAt,
        Instant editedAt,
        List<MessageReactionResponse> reactions
) {
    public static MessageResponse from(Message message) {
        return from(message, List.of());
    }

    public static MessageResponse from(Message message, List<MessageReactionResponse> reactions) {
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getType(),
                message.getBody(),
                message.getCreatedAt(),
                message.getEditedAt(),
                reactions);
    }
}