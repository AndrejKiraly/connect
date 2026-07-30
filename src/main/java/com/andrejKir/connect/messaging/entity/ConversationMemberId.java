package com.andrejKir.connect.messaging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ConversationMemberId implements Serializable {
    @Column(nullable = false)
    private UUID conversationId;
    @Column(nullable = false)
    private UUID appUserId;

    public ConversationMemberId(UUID conversationId, UUID appUserId){
        this.conversationId = conversationId;
        this.appUserId = appUserId;
    }

    protected ConversationMemberId(){}

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getAppUserId() {
        return appUserId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId, appUserId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ConversationMemberId that)) return false;
        return Objects.equals(conversationId, that.conversationId)
                && Objects.equals(appUserId, that.appUserId);
    }
}