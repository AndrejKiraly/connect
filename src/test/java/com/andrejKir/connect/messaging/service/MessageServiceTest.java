package com.andrejKir.connect.messaging.service;

import com.andrejKir.connect.messaging.dto.request.MessageRequest;
import com.andrejKir.connect.messaging.entity.Conversation;
import com.andrejKir.connect.messaging.exception.ConversationNotFoundException;
import com.andrejKir.connect.messaging.exception.NotFriendsException;
import com.andrejKir.connect.messaging.repository.ConversationMemberRepository;
import com.andrejKir.connect.messaging.repository.ConversationRepository;
import com.andrejKir.connect.messaging.repository.MessageRepository;
import com.andrejKir.connect.shared.domain.UserPair;
import com.andrejKir.connect.shared.ratelimit.RateLimitPolicy;
import com.andrejKir.connect.shared.ratelimit.RateLimitService;
import com.andrejKir.connect.social.service.FriendshipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationMemberRepository conversationMemberRepository;
    @Mock
    private FriendshipService friendshipService;
    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private MessageService messageService;

    private final UUID conversationId = UUID.randomUUID();
    private final UUID senderId = UUID.randomUUID();
    private final UUID counterpartId = UUID.randomUUID();

    @Test
    void createMessage_notAMember_isRejected() {
        when(conversationRepository.findForMember(conversationId, senderId)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class, this::sendMessage);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void createMessage_directConversationWithoutFriendship_isRejected() {
        when(conversationRepository.findForMember(conversationId, senderId))
                .thenReturn(Optional.of(directConversation()));
        when(friendshipService.areFriends(senderId, counterpartId)).thenReturn(false);

        assertThrows(NotFriendsException.class, this::sendMessage);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void createMessage_consumesBurstLimitBeforeSustainedAndBeforeAuthorization() {
        when(conversationRepository.findForMember(conversationId, senderId)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class, this::sendMessage);

        InOrder order = inOrder(rateLimitService, conversationRepository);
        order.verify(rateLimitService).check(RateLimitPolicy.MESSAGE_SEND_BURST_PER_USER, senderId.toString());
        order.verify(rateLimitService).check(RateLimitPolicy.MESSAGE_SEND_PER_USER, senderId.toString());
        order.verify(conversationRepository).findForMember(conversationId, senderId);
    }

    private void sendMessage() {
        messageService.createMessage(conversationId, senderId, new MessageRequest("ahoj"));
    }

    private Conversation directConversation() {
        return Conversation.direct(UserPair.of(senderId, counterpartId));
    }
}