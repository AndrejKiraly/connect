package com.andrejKir.connect.social.entity;


import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
public class CircleMember {

    @EmbeddedId
    private CircleMemberId id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected CircleMember () { }

    public CircleMember(UUID circleId, UUID appUserId){
        this.id = new CircleMemberId(circleId,appUserId);
    }

    public CircleMemberId getId() {
        return this.id;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
}
