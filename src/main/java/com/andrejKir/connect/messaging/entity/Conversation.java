package com.andrejKir.connect.messaging.entity;

import com.andrejKir.connect.messaging.enums.ConversationType;
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
}
