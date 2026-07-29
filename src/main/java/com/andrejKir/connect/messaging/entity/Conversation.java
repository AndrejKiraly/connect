package com.andrejKir.connect.messaging.entity;

import com.andrejKir.connect.messaging.enums.ConversationType;
import com.andrejKir.connect.shared.domain.UserPair;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
public class Conversation {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID userLowId;

    private UUID userHighId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConversationType type;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Conversation() {
    }

    private Conversation(ConversationType type, UUID userLowId, UUID userHighId) {
        this.type = type;
        this.userLowId = userLowId;
        this.userHighId = userHighId;
    }

    public static Conversation direct(UserPair pair) {
        return new Conversation(ConversationType.DIRECT, pair.low(), pair.high());
    }

    public UUID getId() {
        return id;
    }

    public ConversationType getType() {
        return type;
    }

    public UUID getUserLowId() {
        return userLowId;
    }

    public UUID getUserHighId() {
        return userHighId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID counterpartOf(UUID userId) {
        if (type != ConversationType.DIRECT) {
            throw new IllegalStateException("Only a direct conversation has a counterpart");
        }
        if (userId.equals(userLowId)) {
            return userHighId;
        }
        if (userId.equals(userHighId)) {
            return userLowId;
        }
        throw new IllegalArgumentException("User is not part of this conversation");
    }
}