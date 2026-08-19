package com.andrejKir.connect.messaging.service;

import com.andrejKir.connect.accounts.dto.response.AppUserPublicSummaryResponse;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.messaging.dto.response.ConversationPageResponse;
import com.andrejKir.connect.messaging.entity.Conversation;
import com.andrejKir.connect.messaging.repository.ConversationInboxRow;
import com.andrejKir.connect.messaging.repository.ConversationMemberRepository;
import com.andrejKir.connect.messaging.repository.ConversationRepository;
import com.andrejKir.connect.shared.domain.UserPair;
import com.andrejKir.connect.social.service.FriendshipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationMemberRepository conversationMemberRepository;
    @Mock
    private AppUserService appUserService;
    @Mock
    private FriendshipService friendshipService;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ConversationService conversationService;

    private final UUID actorId = UUID.randomUUID();
    private final UUID counterpartId = UUID.randomUUID();
    private final UserPair pair = UserPair.of(actorId, counterpartId);

    @Test
    void listConversations_whenMoreRowsThanPageSize_trimsPageAndSetsCursor() {
        List<ConversationInboxRow> rows = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            rows.add(inboxRow());
        }
        rows.add(mock(ConversationInboxRow.class));

        when(conversationRepository.findInbox(eq(actorId), isNull(), eq(""), eq(false), eq(51))).thenReturn(rows);
        when(appUserService.getSummaries(anySet())).thenReturn(Map.of());

        ConversationPageResponse page = conversationService.listConversations(actorId, null, false, null);

        assertEquals(50, page.conversations().size());
        assertEquals(rows.get(49).getLastMessageId(), page.nextCursor());
    }

    private ConversationInboxRow inboxRow() {
        ConversationInboxRow row = mock(ConversationInboxRow.class);
        when(row.getId()).thenReturn(UUID.randomUUID());
        when(row.getType()).thenReturn("DIRECT");
        when(row.getCounterpartId()).thenReturn(counterpartId);
        when(row.getLastMessageId()).thenReturn(UUID.randomUUID());
        when(row.getLastMessageType()).thenReturn("TEXT");
        when(row.getLastMessageSenderId()).thenReturn(counterpartId);
        when(row.getPreview()).thenReturn("ahoj");
        when(row.getLastMessageAt()).thenReturn(Instant.EPOCH);
        return row;
    }

    @Test
    void findOrCreateDirect_whenConcurrentInsertWon_returnsTheWinningConversation() {
        Conversation winner = Conversation.direct(pair);

        when(friendshipService.areFriends(actorId, counterpartId)).thenReturn(true);
        when(conversationRepository.findDirect(pair))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(transactionTemplate.<Conversation>execute(any()))
                .thenThrow(new DataIntegrityViolationException("uq_conversation_direct"));
        when(appUserService.getSummary(counterpartId))
                .thenReturn(new AppUserPublicSummaryResponse(counterpartId, "Lubo Ander", false));

        ConversationService.OpenedConversation opened =
                conversationService.findOrCreateDirect(actorId, counterpartId);

        assertFalse(opened.created());
        verify(conversationRepository, times(2)).findDirect(pair);
    }

    @Test
    void findOrCreateDirect_whenConflictLeavesNoRow_rethrowsOriginalFailure() {
        when(friendshipService.areFriends(actorId, counterpartId)).thenReturn(true);
        when(conversationRepository.findDirect(pair)).thenReturn(Optional.empty());
        when(transactionTemplate.<Conversation>execute(any()))
                .thenThrow(new DataIntegrityViolationException("fk_conversation_user"));

        assertThrows(DataIntegrityViolationException.class,
                () -> conversationService.findOrCreateDirect(actorId, counterpartId));
    }
}
