package com.andrejKir.connect.messaging.repository;

import com.andrejKir.connect.messaging.entity.Message;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByIdDesc(UUID conversationId, Limit limit);

    List<Message> findByConversationIdAndIdLessThanOrderByIdDesc(UUID conversationId, UUID before, Limit limit);
}