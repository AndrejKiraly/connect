package com.andrejKir.connect.messaging.repository;

import com.andrejKir.connect.messaging.entity.MessageReaction;
import com.andrejKir.connect.messaging.entity.MessageReactionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, MessageReactionId> {

    List<MessageReaction> findByIdMessageIdInOrderByCreatedAtAscIdAppUserIdAsc(Collection<UUID> messageIds);
}
