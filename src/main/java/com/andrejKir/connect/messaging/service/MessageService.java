package com.andrejKir.connect.messaging.service;

import com.andrejKir.connect.messaging.dto.request.MessageRequest;
import com.andrejKir.connect.messaging.dto.response.MessageResponse;
import com.andrejKir.connect.messaging.entity.Conversation;
import com.andrejKir.connect.messaging.entity.Message;
import com.andrejKir.connect.messaging.enums.ConversationType;
import com.andrejKir.connect.messaging.exception.ConversationNotFoundException;
import com.andrejKir.connect.messaging.exception.NotFriendsException;
import com.andrejKir.connect.messaging.repository.ConversationMemberRepository;
import com.andrejKir.connect.messaging.repository.ConversationRepository;
import com.andrejKir.connect.messaging.repository.MessageRepository;
import com.andrejKir.connect.shared.ratelimit.RateLimitPolicy;
import com.andrejKir.connect.shared.ratelimit.RateLimitService;
import com.andrejKir.connect.social.service.FriendshipService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final FriendshipService friendshipService;
    private final RateLimitService rateLimitService;

    public MessageService(MessageRepository messageRepository,
                          ConversationRepository conversationRepository,
                          ConversationMemberRepository conversationMemberRepository,
                          FriendshipService friendshipService,
                          RateLimitService rateLimitService) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.friendshipService = friendshipService;
        this.rateLimitService = rateLimitService;
    }

    @Transactional
    public MessageResponse createMessage(UUID conversationId, UUID senderId, MessageRequest request) {
        rateLimitService.check(RateLimitPolicy.MESSAGE_SEND_BURST_PER_USER, senderId.toString());
        rateLimitService.check(RateLimitPolicy.MESSAGE_SEND_PER_USER, senderId.toString());

        Conversation conversation = requireMembership(conversationId, senderId);
        requireMessageAllowed(conversation, senderId);

        Message message = messageRepository.save(Message.text(conversationId, senderId, request.body()));
        conversationMemberRepository.markRead(conversationId, senderId, message.getId());
        return MessageResponse.from(message);
    }

    private Conversation requireMembership(UUID conversationId, UUID actorId) {
        return conversationRepository.findForMember(conversationId, actorId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    private void requireMessageAllowed(Conversation conversation, UUID actorId) {
        if (conversation.getType() != ConversationType.DIRECT) {
            return;
        }
        if (!friendshipService.areFriends(actorId, conversation.counterpartOf(actorId))) {
            throw new NotFriendsException();
        }
    }
}