package com.andrejKir.connect.messaging.repository;

import com.andrejKir.connect.messaging.entity.Conversation;
import com.andrejKir.connect.messaging.enums.ConversationType;
import com.andrejKir.connect.shared.domain.UserPair;
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

    Optional<Conversation> findByTypeAndUserLowIdAndUserHighId(ConversationType type,
                                                              UUID userLowId,
                                                              UUID userHighId);

    default Optional<Conversation> findDirect(UserPair pair) {
        return findByTypeAndUserLowIdAndUserHighId(ConversationType.DIRECT, pair.low(), pair.high());
    }

    @Query(value = """
           SELECT c.id                         AS "id",
                  c.type                       AS "type",
                  cp.id                        AS "counterpartId",
                  lm.id                        AS "lastMessageId",
                  lm.type                      AS "lastMessageType",
                  lm.sender_id                 AS "lastMessageSenderId",
                  left(lm.body, 140)           AS "preview",
                  (char_length(lm.body) > 140) AS "truncated",
                  lm.created_at                AS "lastMessageAt",
                  (cm.last_read_message_id IS NULL
                   OR lm.id > cm.last_read_message_id) AS "unread"
             FROM conversation_member cm
             JOIN conversation c ON c.id = cm.conversation_id
             JOIN LATERAL (SELECT m.id, m.type, m.sender_id, m.body, m.created_at
                             FROM message m
                            WHERE m.conversation_id = cm.conversation_id
                            ORDER BY m.id DESC
                            LIMIT 1) lm ON true
             CROSS JOIN LATERAL (SELECT CASE WHEN c.user_low_id = cm.app_user_id
                                             THEN c.user_high_id ELSE c.user_low_id END) cp(id)
            WHERE cm.app_user_id = :actorId
              AND (CAST(:cursor AS uuid) IS NULL OR lm.id < CAST(:cursor AS uuid))
              AND (NOT CAST(:unreadOnly AS boolean)
                   OR cm.last_read_message_id IS NULL
                   OR lm.id > cm.last_read_message_id)
              AND (:query = '' OR EXISTS (SELECT 1
                                            FROM app_user u
                                           WHERE u.id = cp.id
                                             AND u.deactivated_at IS NULL
                                             AND strpos(lower(unaccent(u.display_name)),
                                                        lower(unaccent(:query))) > 0))
            ORDER BY lm.id DESC
            LIMIT :limit
           """, nativeQuery = true)
    List<ConversationInboxRow> findInbox(@Param("actorId") UUID actorId,
                                         @Param("cursor") UUID cursor,
                                         @Param("query") String query,
                                         @Param("unreadOnly") boolean unreadOnly,
                                         @Param("limit") int limit);
}
