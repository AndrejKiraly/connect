package com.andrejKir.connect.messaging.service;

import com.andrejKir.connect.accounts.dto.response.AppUserPublicSummaryResponse;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.messaging.dto.request.MessageRequest;
import com.andrejKir.connect.messaging.dto.response.MessagePageResponse;
import com.andrejKir.connect.messaging.dto.response.MessageReactionResponse;
import com.andrejKir.connect.messaging.dto.response.MessageResponse;
import com.andrejKir.connect.messaging.entity.Conversation;
import com.andrejKir.connect.messaging.entity.Message;
import com.andrejKir.connect.messaging.entity.MessageReaction;
import com.andrejKir.connect.messaging.enums.ConversationType;
import com.andrejKir.connect.messaging.enums.MessageReactionType;
import com.andrejKir.connect.messaging.exception.NotFriendsException;
import com.andrejKir.connect.messaging.repository.ConversationMemberRepository;
import com.andrejKir.connect.messaging.repository.MessageReactionRepository;
import com.andrejKir.connect.messaging.repository.MessageRepository;
import com.andrejKir.connect.shared.ratelimit.RateLimitPolicy;
import com.andrejKir.connect.shared.ratelimit.RateLimitService;
import com.andrejKir.connect.social.service.FriendshipService;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final int MESSAGE_PAGE_SIZE = 50;

    private final MessageRepository messageRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ConversationService conversationService;
    private final AppUserService appUserService;
    private final FriendshipService friendshipService;
    private final RateLimitService rateLimitService;

    public MessageService(MessageRepository messageRepository,
                          MessageReactionRepository messageReactionRepository,
                          ConversationMemberRepository conversationMemberRepository,
                          ConversationService conversationService,
                          AppUserService appUserService,
                          FriendshipService friendshipService,
                          RateLimitService rateLimitService) {
        this.messageRepository = messageRepository;
        this.messageReactionRepository = messageReactionRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.conversationService = conversationService;
        this.appUserService = appUserService;
        this.friendshipService = friendshipService;
        this.rateLimitService = rateLimitService;
    }

    @Transactional
    public MessageResponse createMessage(UUID conversationId, UUID senderId, MessageRequest request) {
        rateLimitService.check(RateLimitPolicy.MESSAGE_SEND_BURST_PER_USER, senderId.toString());
        rateLimitService.check(RateLimitPolicy.MESSAGE_SEND_PER_USER, senderId.toString());

        Conversation conversation = conversationService.requireMember(conversationId, senderId);
        requireMessageAllowed(conversation, senderId);

        Message message = messageRepository.save(Message.text(conversationId, senderId, request.body()));
        conversationMemberRepository.markRead(conversationId, senderId, message.getId());
        return MessageResponse.from(message);
    }

    @Transactional(readOnly = true)
    public MessagePageResponse listMessages(UUID conversationId, UUID actorId, UUID cursor) {
        conversationService.requireMember(conversationId, actorId);

        Limit limit = Limit.of(MESSAGE_PAGE_SIZE + 1);
        List<Message> page = cursor == null
                ? messageRepository.findByConversationIdOrderByIdDesc(conversationId, limit)
                : messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(conversationId, cursor, limit);

        boolean hasMore = page.size() > MESSAGE_PAGE_SIZE;
        if (hasMore) {
            page = page.subList(0, MESSAGE_PAGE_SIZE);
        }

        List<MessageReaction> reactions = findReactions(page);
        Map<UUID, List<MessageReactionResponse>> reactionsByMessage = groupByMessage(reactions);
        Map<UUID, AppUserPublicSummaryResponse> users = appUserService.getSummaries(referencedUserIds(page, reactions));

        List<MessageResponse> messages = page.stream()
                .map(message -> MessageResponse.from(message, reactionsByMessage.getOrDefault(message.getId(), List.of())))
                .toList();

        return MessagePageResponse.of(messages, users, hasMore);
    }

    private List<MessageReaction> findReactions(List<Message> page) {
        if (page.isEmpty()) {
            return List.of();
        }
        return messageReactionRepository.findByIdMessageIdInOrderByCreatedAtAscIdAppUserIdAsc(
                page.stream().map(Message::getId).toList());
    }

    private Map<UUID, List<MessageReactionResponse>> groupByMessage(List<MessageReaction> reactions) {
        Map<UUID, Map<MessageReactionType, List<UUID>>> grouped = new HashMap<>();
        for (MessageReaction reaction : reactions) {
            grouped.computeIfAbsent(reaction.getId().getMessageId(), key -> new EnumMap<>(MessageReactionType.class))
                    .computeIfAbsent(reaction.getReactionType(), key -> new ArrayList<>())
                    .add(reaction.getId().getAppUserId());
        }
        return grouped.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> toResponses(entry.getValue())));
    }

    private List<MessageReactionResponse> toResponses(Map<MessageReactionType, List<UUID>> byType) {
        return byType.entrySet().stream()
                .map(entry -> new MessageReactionResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Set<UUID> referencedUserIds(List<Message> page, List<MessageReaction> reactions) {
        Set<UUID> userIds = new HashSet<>();
        for (Message message : page) {
            userIds.add(message.getSenderId());
        }
        for (MessageReaction reaction : reactions) {
            userIds.add(reaction.getId().getAppUserId());
        }
        return userIds;
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