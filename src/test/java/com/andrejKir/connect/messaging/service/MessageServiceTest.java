package com.andrejKir.connect.messaging.service;

import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.messaging.dto.request.MessageRequest;
import com.andrejKir.connect.messaging.entity.Conversation;
import com.andrejKir.connect.messaging.exception.ConversationNotFoundException;
import com.andrejKir.connect.messaging.exception.NotFriendsException;
import com.andrejKir.connect.messaging.repository.ConversationMemberRepository;
import com.andrejKir.connect.messaging.repository.MessageReactionRepository;
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
    private MessageReactionRepository messageReactionRepository;
    @Mock
    private ConversationMemberRepository conversationMemberRepository;
    @Mock
    private ConversationService conversationService;
    @Mock
    private AppUserService appUserService;
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
        when(conversationService.requireMember(conversationId, senderId))
                .thenThrow(new ConversationNotFoundException(conversationId));

        assertThrows(ConversationNotFoundException.class, this::sendMessage);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void createMessage_directConversationWithoutFriendship_isRejected() {
        when(conversationService.requireMember(conversationId, senderId)).thenReturn(directConversation());
        when(friendshipService.areFriends(senderId, counterpartId)).thenReturn(false);

        assertThrows(NotFriendsException.class, this::sendMessage);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void createMessage_consumesBurstLimitBeforeSustainedAndBeforeAuthorization() {
        when(conversationService.requireMember(conversationId, senderId))
                .thenThrow(new ConversationNotFoundException(conversationId));

        assertThrows(ConversationNotFoundException.class, this::sendMessage);

        InOrder order = inOrder(rateLimitService, conversationService);
        order.verify(rateLimitService).check(RateLimitPolicy.MESSAGE_SEND_BURST_PER_USER, senderId.toString());
        order.verify(rateLimitService).check(RateLimitPolicy.MESSAGE_SEND_PER_USER, senderId.toString());
        order.verify(conversationService).requireMember(conversationId, senderId);
    }

    private void sendMessage() {
        messageService.createMessage(conversationId, senderId, new MessageRequest("ahoj"));
    }

    private Conversation directConversation() {
        return Conversation.direct(UserPair.of(senderId, counterpartId));
    }
}