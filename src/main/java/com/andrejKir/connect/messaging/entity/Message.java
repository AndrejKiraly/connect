package com.andrejKir.connect.messaging.entity;


import com.andrejKir.connect.messaging.enums.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
public class Message {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false)
    private UUID senderId;

    @Column(nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageType type;

    @Column(nullable = false)
    private String body;

    private Instant editedAt;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    protected Message() {
    }

    private Message(UUID conversationId, UUID senderId, MessageType type, String body) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.type = type;
        this.body = body;
    }

    public static Message text(UUID conversationId, UUID senderId, String body) {
        return new Message(conversationId, senderId, MessageType.TEXT, body);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public MessageType getType() {
        return type;
    }

    public String getBody() {
        return body;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}