package com.andrejKir.connect.messaging.repository;

import java.time.Instant;
import java.util.UUID;

public interface ConversationInboxRow {

    UUID getId();

    String getType();

    UUID getCounterpartId();

    UUID getLastMessageId();

    String getLastMessageType();

    UUID getLastMessageSenderId();

    String getPreview();

    boolean isTruncated();

    Instant getLastMessageAt();

    boolean isUnread();
}
