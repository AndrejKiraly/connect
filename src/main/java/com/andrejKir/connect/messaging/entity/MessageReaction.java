package com.andrejKir.connect.messaging.entity;

import com.andrejKir.connect.messaging.enums.MessageReactionType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
public class MessageReaction {

    @EmbeddedId
    private MessageReactionId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageReactionType reactionType;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    protected MessageReaction() {
    }

    private MessageReaction(MessageReactionId id, MessageReactionType reactionType) {
        this.id = id;
        this.reactionType = reactionType;
    }

    public static MessageReaction of(UUID messageId, UUID appUserId, MessageReactionType reactionType) {
        return new MessageReaction(new MessageReactionId(messageId, appUserId), reactionType);
    }

    public MessageReactionId getId() {
        return id;
    }

    public MessageReactionType getReactionType() {
        return reactionType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}