package com.andrejKir.connect.accounts.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
public class PasswordResetToken {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;


    protected PasswordResetToken(){}

    public PasswordResetToken(UUID userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public void markUsed(Instant now){
        this.usedAt = now;
    }
    public boolean isUsed() {
        return this.usedAt != null;
    }
    public boolean isExpired(Instant now){
        return expiresAt.isBefore(now);
    }

    public UUID getUserId() {
        return userId;
    }
}
