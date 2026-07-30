package com.andrejKir.connect.messaging.repository;

import com.andrejKir.connect.messaging.entity.ConversationMember;
import com.andrejKir.connect.messaging.entity.ConversationMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {

    List<ConversationMember> findByIdAppUserId(UUID appUserId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
           UPDATE conversation_member cm
              SET last_read_message_id = :messageId
            WHERE cm.conversation_id = :conversationId
              AND cm.app_user_id = :appUserId
              AND (cm.last_read_message_id IS NULL OR cm.last_read_message_id < :messageId)
              AND EXISTS (SELECT 1 FROM message m
                           WHERE m.id = :messageId
                             AND m.conversation_id = :conversationId)
           """, nativeQuery = true)
    int markRead(@Param("conversationId") UUID conversationId,
                 @Param("appUserId") UUID appUserId,
                 @Param("messageId") UUID messageId);
}
