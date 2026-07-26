package com.andrejKir.connect.messaging.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(nullable = false)
    private String body;

    private Instant editedAt;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;
}
