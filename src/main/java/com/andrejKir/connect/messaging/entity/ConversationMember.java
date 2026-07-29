package com.andrejKir.connect.messaging.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
public class ConversationMember{

    @EmbeddedId
    private ConversationMemberId id;

    private UUID lastReadMessageId;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    protected ConversationMember() {
    }

    public ConversationMember(UUID conversationId, UUID appUserId) {
        this.id = new ConversationMemberId(appUserId, conversationId);
    }

    public ConversationMemberId getId() {
        return id;
    }

    public UUID getLastReadMessageId() {
        return lastReadMessageId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
