package com.andrejKir.connect.messaging.entity;

import com.andrejKir.connect.messaging.enums.MessageReactionType;
import jakarta.persistence.*;
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