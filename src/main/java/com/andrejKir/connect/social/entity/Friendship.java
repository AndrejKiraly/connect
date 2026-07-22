package com.andrejKir.connect.social.entity;

import com.andrejKir.connect.shared.domain.UserPair;
import com.andrejKir.connect.social.enums.FriendshipStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
public class Friendship {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private FriendshipStatus status;

    @Column(nullable = false)
    private UUID userHighId;

    @Column(nullable = false)
    private UUID userLowId;

    @Column(nullable = false)
    private UUID requestedBy;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    protected Friendship(){ }

    private Friendship(UUID userLowId, UUID userHighId, UUID requestedBy){
        this.userLowId   = userLowId;
        this.userHighId  = userHighId;
        this.requestedBy = requestedBy;
        this.status      = FriendshipStatus.PENDING;
    }
    public static Friendship request(UserPair pair, UUID requesterId) {
        return new Friendship(pair.low(), pair.high(), requesterId);
    }

    public void accept() {
        if (status != FriendshipStatus.PENDING)
            throw new IllegalStateException("Only a pending request can be accepted");
        this.status = FriendshipStatus.ACCEPTED;
    }
    public void decline() {
        if (status != FriendshipStatus.PENDING)
            throw new IllegalStateException("Only a pending request can be declined");
        this.status = FriendshipStatus.DECLINED;
    }

    public UUID getId() {
        return id;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public UUID getUserHighId() {
        return userHighId;
    }

    public UUID getUserLowId() {
        return userLowId;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean involves(UUID userId){
        return userLowId.equals(userId) || userHighId.equals(userId);
    }

    public boolean isRequestedBy(UUID userId){
        return requestedBy.equals(userId);
    }
}
