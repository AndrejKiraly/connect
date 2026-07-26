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
}
