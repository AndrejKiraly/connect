package com.andrejKir.connect.messaging.repository;

import com.andrejKir.connect.messaging.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query( value = """
                    SELECT m.id AS "id",
                           m.body as "body",
                           
                                        FROM Message m


                       """, nativeQuery = true
    )
    List<Message> findByFirstNConversationId(int count, UUID conversationId);
}
