package com.andrejKir.connect.messaging.entity;

import com.andrejKir.connect.messaging.enums.MessageReactionType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

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
}