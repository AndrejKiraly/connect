package com.andrejKir.connect.messaging.service;

import com.andrejKir.connect.accounts.dto.response.AppUserPublicSummaryResponse;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.messaging.dto.response.ConversationSummaryResponse;
import com.andrejKir.connect.messaging.repository.ConversationInboxRow;
import com.andrejKir.connect.messaging.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ConversationService {

    private static final int INBOX_PAGE_SIZE = 50;

    private final ConversationRepository conversationRepository;
    private final AppUserService appUserService;

    public ConversationService(ConversationRepository conversationRepository, AppUserService appUserService) {
        this.conversationRepository = conversationRepository;
        this.appUserService = appUserService;
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> listConversations(UUID actorId) {
        List<ConversationInboxRow> rows = conversationRepository.findInbox(actorId, INBOX_PAGE_SIZE);
        Map<UUID, AppUserPublicSummaryResponse> users = appUserService.getSummaries(referencedUserIds(rows));

        return rows.stream()
                .map(row -> ConversationSummaryResponse.from(row, users, actorId))
                .toList();
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