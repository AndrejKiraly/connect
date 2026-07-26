package com.andrejKir.connect.messaging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ConversationMemberId implements Serializable {
    @Column(nullable = false)
    private UUID appUserId;
    @Column(nullable = false)
    private UUID conversationId;

    public ConversationMemberId(UUID appUserId, UUID conversationId){
        this.conversationId = conversationId;
        this.appUserId = appUserId;
    }

    protected ConversationMemberId(){}

    public UUID getAppUserId() {
        return appUserId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(appUserId, conversationId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ConversationMemberId that)) return false;
        return Objects.equals(appUserId, that.appUserId)
                && Objects.equals(conversationId, that.conversationId);
    }
}