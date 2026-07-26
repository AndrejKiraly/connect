package com.andrejKir.connect.messaging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class MessageReactionId implements Serializable {

    @Column(nullable = false)
    private UUID messageId;

    @Column(nullable = false)
    private UUID appUserId;

    public MessageReactionId(UUID messageId, UUID appUserId) {
        this.messageId = messageId;
        this.appUserId = appUserId;
    }

    protected MessageReactionId() {
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getAppUserId() {
        return appUserId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, appUserId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MessageReactionId that)) return false;
        return Objects.equals(messageId, that.messageId)
                && Objects.equals(appUserId, that.appUserId);
    }
}