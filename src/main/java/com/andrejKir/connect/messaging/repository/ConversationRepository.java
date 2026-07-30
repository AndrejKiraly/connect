package com.andrejKir.connect.messaging.repository;

import com.andrejKir.connect.messaging.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
           select c from Conversation c
            where c.id = :conversationId
              and exists (select 1 from ConversationMember m
                           where m.id.conversationId = c.id
                             and m.id.appUserId = :actorId)
           """)
    Optional<Conversation> findForMember(@Param("conversationId") UUID conversationId,
                                         @Param("actorId") UUID actorId);

    @Query(value = """
           SELECT c.id                         AS "id",
                  c.type                       AS "type",
                  cp.app_user_id               AS "counterpartId",
                  lm.id                        AS "lastMessageId",
                  lm.type                      AS "lastMessageType",
                  lm.sender_id                 AS "lastMessageSenderId",
                  left(lm.body, 140)           AS "preview",
                  (char_length(lm.body) > 140) AS "truncated",
                  lm.created_at                AS "lastMessageAt",
                  (lm.id > COALESCE(cm.last_read_message_id,
                                    '00000000-0000-0000-0000-000000000000'::uuid))
                                               AS "unread"
             FROM conversation_member cm
             JOIN conversation c ON c.id = cm.conversation_id
             JOIN LATERAL (SELECT m.id, m.type, m.sender_id, m.body, m.created_at
                             FROM message m
                            WHERE m.conversation_id = cm.conversation_id
                            ORDER BY m.id DESC
                            LIMIT 1) lm ON true
             LEFT JOIN LATERAL (SELECT o.app_user_id
                                  FROM conversation_member o
                                 WHERE o.conversation_id = cm.conversation_id
                                   AND o.app_user_id <> cm.app_user_id
                                 LIMIT 1) cp ON c.type = 'DIRECT'
            WHERE cm.app_user_id = :actorId
            ORDER BY lm.created_at DESC, c.id DESC
            LIMIT :limit
           """, nativeQuery = true)
    List<ConversationInboxRow> findInbox(@Param("actorId") UUID actorId, @Param("limit") int limit);
}