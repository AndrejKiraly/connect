package com.andrejKir.connect.messaging.service;

import com.andrejKir.connect.accounts.dto.response.AppUserPublicSummaryResponse;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.messaging.dto.response.ConversationDetailResponse;
import com.andrejKir.connect.messaging.dto.response.ConversationPageResponse;
import com.andrejKir.connect.messaging.dto.response.ConversationSummaryResponse;
import com.andrejKir.connect.messaging.entity.Conversation;
import com.andrejKir.connect.messaging.entity.ConversationMember;
import com.andrejKir.connect.messaging.enums.ConversationType;
import com.andrejKir.connect.messaging.exception.ConversationNotFoundException;
import com.andrejKir.connect.messaging.exception.NotFriendsException;
import com.andrejKir.connect.messaging.exception.SelfConversationException;
import com.andrejKir.connect.messaging.repository.ConversationInboxRow;
import com.andrejKir.connect.messaging.repository.ConversationMemberRepository;
import com.andrejKir.connect.messaging.repository.ConversationRepository;
import com.andrejKir.connect.shared.domain.UserPair;
import com.andrejKir.connect.social.service.FriendshipService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ConversationService {

    private static final int INBOX_PAGE_SIZE = 50;
    private static final int SEARCH_QUERY_MAX_LENGTH = 100;

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final AppUserService appUserService;
    private final FriendshipService friendshipService;
    private final TransactionTemplate transactionTemplate;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMemberRepository conversationMemberRepository,
                               AppUserService appUserService,
                               FriendshipService friendshipService,
                               TransactionTemplate transactionTemplate) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.appUserService = appUserService;
        this.friendshipService = friendshipService;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(readOnly = true)
    public ConversationPageResponse listConversations(UUID actorId, String query, boolean unreadOnly, UUID cursor) {
        List<ConversationInboxRow> rows = conversationRepository.findInbox(
                actorId, cursor, normalizeQuery(query), unreadOnly, INBOX_PAGE_SIZE + 1);

        boolean hasMore = rows.size() > INBOX_PAGE_SIZE;
        if (hasMore) {
            rows = rows.subList(0, INBOX_PAGE_SIZE);
        }
        UUID nextCursor = hasMore ? rows.getLast().getLastMessageId() : null;

        Map<UUID, AppUserPublicSummaryResponse> users = appUserService.getSummaries(referencedUserIds(rows));

        List<ConversationSummaryResponse> conversations = rows.stream()
                .map(row -> ConversationSummaryResponse.from(row, users, actorId))
                .toList();

        return new ConversationPageResponse(conversations, nextCursor);
    }

    @Transactional(readOnly = true)
    public Conversation requireMember(UUID conversationId, UUID actorId) {
        return conversationRepository.findForMember(conversationId, actorId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversation(UUID conversationId, UUID actorId) {
        Conversation conversation = requireMember(conversationId, actorId);

        AppUserPublicSummaryResponse counterpart = conversation.getType() == ConversationType.DIRECT
                ? appUserService.getSummary(conversation.counterpartOf(actorId))
                : null;

        return ConversationDetailResponse.from(conversation, counterpart);
    }

    @Transactional
    public void markRead(UUID conversationId, UUID actorId, UUID lastReadMessageId) {
        requireMember(conversationId, actorId);
        conversationMemberRepository.markRead(conversationId, actorId, lastReadMessageId);
    }

    public OpenedConversation findOrCreateDirect(UUID actorId, UUID counterpartId) {
        if (actorId.equals(counterpartId)) {
            throw new SelfConversationException();
        }
        if (!friendshipService.areFriends(actorId, counterpartId)) {
            throw new NotFriendsException();
        }

        UserPair userPair = UserPair.of(actorId, counterpartId);

        Optional<Conversation> existing = conversationRepository.findDirect(userPair);
        if (existing.isPresent()) {
            return toResult(existing.get(), counterpartId, false);
        }

        try {
            Conversation created = transactionTemplate.execute(status -> {
                Conversation saved = conversationRepository.save(Conversation.direct(userPair));
                conversationMemberRepository.saveAll(List.of(
                        new ConversationMember(saved.getId(), userPair.low()),
                        new ConversationMember(saved.getId(), userPair.high())));
                return saved;
            });
            return toResult(created, counterpartId, true);
        } catch (DataIntegrityViolationException e) {
            return toResult(conversationRepository.findDirect(userPair).orElseThrow(() -> e), counterpartId, false);
        }
    }

    public record OpenedConversation(ConversationDetailResponse conversation, boolean created){}

    private OpenedConversation toResult(Conversation conversation, UUID counterpartId, boolean created) {
        return new OpenedConversation(
                ConversationDetailResponse.from(conversation, appUserService.getSummary(counterpartId)),
                created);
    }

    private static String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        String trimmed = query.trim();
        return trimmed.length() > SEARCH_QUERY_MAX_LENGTH
                ? trimmed.substring(0, SEARCH_QUERY_MAX_LENGTH)
                : trimmed;
    }

    private Set<UUID> referencedUserIds(List<ConversationInboxRow> rows) {
        Set<UUID> userIds = new HashSet<>();
        for (ConversationInboxRow row : rows) {
            if (row.getCounterpartId() != null) {
                userIds.add(row.getCounterpartId());
            }
            userIds.add(row.getLastMessageSenderId());
        }
        return userIds;
    }
}
