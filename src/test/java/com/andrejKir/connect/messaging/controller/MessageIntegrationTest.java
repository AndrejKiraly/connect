package com.andrejKir.connect.messaging.controller;

import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserPrivateSummaryResponse;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.messaging.entity.Conversation;
import com.andrejKir.connect.messaging.entity.ConversationMember;
import com.andrejKir.connect.messaging.entity.ConversationMemberId;
import com.andrejKir.connect.messaging.entity.Message;
import com.andrejKir.connect.messaging.entity.MessageReaction;
import com.andrejKir.connect.messaging.enums.MessageReactionType;
import com.andrejKir.connect.messaging.repository.ConversationMemberRepository;
import com.andrejKir.connect.messaging.repository.ConversationRepository;
import com.andrejKir.connect.messaging.repository.MessageReactionRepository;
import com.andrejKir.connect.messaging.repository.MessageRepository;
import com.andrejKir.connect.shared.domain.UserPair;
import com.andrejKir.connect.social.entity.Friendship;
import com.andrejKir.connect.social.repository.FriendshipRepository;
import com.andrejKir.connect.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MessageIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    AppUserService appUserService;
    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    FriendshipRepository friendshipRepository;
    @Autowired
    ConversationRepository conversationRepository;
    @Autowired
    ConversationMemberRepository conversationMemberRepository;
    @Autowired
    MessageRepository messageRepository;
    @Autowired
    MessageReactionRepository messageReactionRepository;

    private AppUserPrivateSummaryResponse ana;
    private AppUserPrivateSummaryResponse bob;
    private UUID conversationId;

    @Autowired
    TransactionTemplate transactionTemplate;

    @AfterEach
    void clearMessaging() {
        messageReactionRepository.deleteAllInBatch();
        messageRepository.deleteAllInBatch();
        conversationMemberRepository.deleteAllInBatch();
        conversationRepository.deleteAllInBatch();
    }

    @BeforeEach
    void seed() {
        ana = register("ana");
        bob = register("bob");

        befriend(ana.id(), bob.id());
        conversationId = openDirectConversation(ana.id(), bob.id());
    }

    @Test
    void sendMessage_persistsMessageAndAdvancesSenderWatermark() throws Exception {
        Cookie session = loginAs(ana.username());

        String body = sendMessage(session, conversationId, "ahoj")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("ahoj"))
                .andExpect(jsonPath("$.senderId").value(ana.id().toString()))
                .andReturn().getResponse().getContentAsString();

        UUID messageId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        assertEquals(messageId, watermarkOf(conversationId, ana.id()));
        assertNull(watermarkOf(conversationId, bob.id()));
    }

    @Test
    void sendMessage_intoConversationOfOthers_returns404() throws Exception {
        Cookie session = loginAs(register("stranger").username());

        sendMessage(session, conversationId, "kde som")
                .andExpect(status().isNotFound());

        assertEquals(0, messageRepository.count());
    }

    @Test
    void markRead_doesNotMoveWatermarkBackwards() {
        UUID older = persistMessage(conversationId, ana.id());
        UUID newer = persistMessage(conversationId, ana.id());

        markRead(conversationId, ana.id(), newer);
        markRead(conversationId, ana.id(), older);

        assertEquals(newer, watermarkOf(conversationId, ana.id()));
    }

    @Test
    void markRead_ignoresMessageFromAnotherConversation() {
        UUID otherConversationId = openDirectConversation(bob.id(), register("cyril").id());
        UUID foreignMessageId = persistMessage(otherConversationId, bob.id());

        markRead(conversationId, ana.id(), foreignMessageId);

        assertNull(watermarkOf(conversationId, ana.id()));
    }

    @Test
    void conversationDetail_exposesTypeAndCounterpart() throws Exception {
        mockMvc.perform(get("/api/v1/conversations/" + conversationId).cookie(loginAs(ana.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.type").value("DIRECT"))
                .andExpect(jsonPath("$.counterpart.id").value(bob.id().toString()))
                .andExpect(jsonPath("$.counterpart.displayName").value(bob.username()));
    }

    @Test
    void conversationDetail_ofOthers_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/conversations/" + conversationId)
                        .cookie(loginAs(register("stranger").username())))
                .andExpect(status().isNotFound());
    }

    @Test
    void read_conversationOfOthers_returns404() throws Exception {
        UUID messageId = persistMessage(conversationId, ana.id());
        Cookie session = loginAs(register("stranger").username());

        postRead(session, conversationId, messageId)
                .andExpect(status().isNotFound());

        assertNull(watermarkOf(conversationId, ana.id()));
        assertNull(watermarkOf(conversationId, bob.id()));
    }

    @Test
    void read_clearsUnreadInInbox() throws Exception {
        Cookie session = loginAs(ana.username());
        UUID messageId = persistMessage(conversationId, bob.id(), "ahoj");

        expectUnread(session, true);

        postRead(session, conversationId, messageId)
                .andExpect(status().isNoContent());

        expectUnread(session, false);
        assertEquals(messageId, watermarkOf(conversationId, ana.id()));
    }

    @Test
    void inbox_listsOnlyMyConversations_withCounterpartAndLastMessage() throws Exception {
        persistMessage(conversationId, bob.id(), "prva");
        UUID last = persistMessage(conversationId, bob.id(), "posledna");

        AppUserPrivateSummaryResponse cyril = register("cyril");
        UUID foreign = openDirectConversation(bob.id(), cyril.id());
        persistMessage(foreign, cyril.id(), "cudzia");

        mockMvc.perform(get("/api/v1/conversations").cookie(loginAs(ana.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations.length()").value(1))
                .andExpect(jsonPath("$.conversations[0].id").value(conversationId.toString()))
                .andExpect(jsonPath("$.conversations[0].counterpart.id").value(bob.id().toString()))
                .andExpect(jsonPath("$.conversations[0].lastMessage.id").value(last.toString()))
                .andExpect(jsonPath("$.conversations[0].lastMessage.preview").value("posledna"))
                .andExpect(jsonPath("$.conversations[0].lastMessage.sentByMe").value(false));
    }

    @Test
    void inbox_ordersConversationsByMostRecentMessage() throws Exception {
        UUID withCyril = openDirectConversation(ana.id(), register("cyril").id());

        persistMessage(withCyril, ana.id(), "starsia");
        persistMessage(conversationId, bob.id(), "novsia");

        mockMvc.perform(get("/api/v1/conversations").cookie(loginAs(ana.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations[0].id").value(conversationId.toString()))
                .andExpect(jsonPath("$.conversations[1].id").value(withCyril.toString()));
    }

    @Test
    void inbox_unreadFollowsReadWatermark() throws Exception {
        Cookie session = loginAs(ana.username());
        UUID older = persistMessage(conversationId, bob.id(), "stara");
        UUID newer = persistMessage(conversationId, bob.id(), "nova");

        expectUnread(session, true);

        markRead(conversationId, ana.id(), older);
        expectUnread(session, true);

        markRead(conversationId, ana.id(), newer);
        expectUnread(session, false);
    }

    @Test
    void inbox_truncatesPreviewOverLimit() throws Exception {
        Cookie session = loginAs(ana.username());

        persistMessage(conversationId, bob.id(), "x".repeat(140));
        mockMvc.perform(get("/api/v1/conversations").cookie(session))
                .andExpect(jsonPath("$.conversations[0].lastMessage.preview").value("x".repeat(140)))
                .andExpect(jsonPath("$.conversations[0].lastMessage.truncated").value(false));

        persistMessage(conversationId, bob.id(), "y".repeat(141));
        mockMvc.perform(get("/api/v1/conversations").cookie(session))
                .andExpect(jsonPath("$.conversations[0].lastMessage.preview").value("y".repeat(140)))
                .andExpect(jsonPath("$.conversations[0].lastMessage.truncated").value(true));
    }

    @Test
    void listMessages_returnsNewestFirstAndPagesByCursor() throws Exception {
        List<UUID> ids = persistMessages(conversationId, ana.id(), 51);
        Cookie session = loginAs(ana.username());

        String firstPage = getMessages(session, conversationId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(50))
                .andExpect(jsonPath("$.messages[0].id").value(ids.getLast().toString()))
                .andReturn().getResponse().getContentAsString();

        UUID cursor = UUID.fromString(objectMapper.readTree(firstPage).get("nextCursor").asText());
        assertEquals(ids.get(1), cursor);

        getMessages(session, conversationId, cursor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].id").value(ids.getFirst().toString()))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void listMessages_conversationOfOthers_returns404() throws Exception {
        persistMessage(conversationId, ana.id());

        getMessages(loginAs(register("stranger").username()), conversationId, null)
                .andExpect(status().isNotFound());
    }

    @Test
    void listMessages_groupsReactionsAndResolvesReactorNames() throws Exception {
        UUID older = persistMessage(conversationId, ana.id(), "prva");
        UUID newer = persistMessage(conversationId, ana.id(), "druha");

        react(older, ana.id(), MessageReactionType.LOVE);
        react(older, bob.id(), MessageReactionType.HAHA);
        react(newer, ana.id(), MessageReactionType.LOVE);
        react(newer, bob.id(), MessageReactionType.LOVE);

        getMessages(loginAs(ana.username()), conversationId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].reactions.length()").value(1))
                .andExpect(jsonPath("$.messages[0].reactions[0].reactionType").value("LOVE"))
                .andExpect(jsonPath("$.messages[0].reactions[0].userIds.length()").value(2))
                .andExpect(jsonPath("$.messages[1].reactions.length()").value(2))
                .andExpect(jsonPath("$.messages[1].reactions[0].reactionType").value("HAHA"))
                .andExpect(jsonPath("$.messages[1].reactions[1].reactionType").value("LOVE"))
                .andExpect(jsonPath("$.users['" + bob.id() + "'].displayName").value(bob.username()));
    }

    private void react(UUID messageId, UUID appUserId, MessageReactionType reactionType) {
        messageReactionRepository.save(MessageReaction.of(messageId, appUserId, reactionType));
    }

    private ResultActions postRead(Cookie session, UUID conversationId, UUID lastReadMessageId) throws Exception {
        return mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/read")
                .with(csrfToken())
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lastReadMessageId\":\"" + lastReadMessageId + "\"}"));
    }

    private ResultActions getMessages(Cookie session, UUID conversationId, UUID cursor) throws Exception {
        String url = "/api/v1/conversations/" + conversationId + "/messages"
                + (cursor == null ? "" : "?cursor=" + cursor);
        return mockMvc.perform(get(url).cookie(session));
    }

    private void expectUnread(Cookie session, boolean unread) throws Exception {
        mockMvc.perform(get("/api/v1/conversations").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations[0].unread").value(unread));
    }

    private void markRead(UUID conversationId, UUID appUserId, UUID messageId) {
        transactionTemplate.executeWithoutResult(
                status -> conversationMemberRepository.markRead(conversationId, appUserId, messageId));
    }

    private ResultActions sendMessage(Cookie session, UUID conversationId, String body)
            throws Exception {
        return mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/messages")
                .with(csrfToken())
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"" + body + "\"}"));
    }

    private UUID watermarkOf(UUID conversationId, UUID appUserId) {
        return conversationMemberRepository.findById(new ConversationMemberId(conversationId, appUserId))
                .orElseThrow()
                .getLastReadMessageId();
    }

    private UUID persistMessage(UUID conversationId, UUID senderId) {
        return persistMessage(conversationId, senderId, "x");
    }

    private List<UUID> persistMessages(UUID conversationId, UUID senderId, int count) {
        return transactionTemplate.execute(status -> {
            List<UUID> ids = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                ids.add(persistMessage(conversationId, senderId, "sprava " + i));
            }
            return ids;
        });
    }

    private UUID persistMessage(UUID conversationId, UUID senderId, String body) {
        return messageRepository.save(Message.text(conversationId, senderId, body)).getId();
    }

    private UUID openDirectConversation(UUID a, UUID b) {
        UserPair pair = UserPair.of(a, b);
        Conversation conversation = conversationRepository.save(Conversation.direct(pair));
        conversationMemberRepository.save(new ConversationMember(conversation.getId(), a));
        conversationMemberRepository.save(new ConversationMember(conversation.getId(), b));
        return conversation.getId();
    }

    private void befriend(UUID a, UUID b) {
        Friendship friendship = Friendship.request(UserPair.of(a, b), a);
        friendship.accept();
        friendshipRepository.save(friendship);
    }

    private AppUserPrivateSummaryResponse register(String prefix) {
        String username = prefix + UUID.randomUUID().toString().substring(0, 8);
        return appUserService.registerUser(new RegisterRequest(
                username + "@example.com", username, PASSWORD, username,
                "First", "Last", LocalDate.of(2000, 1, 1)));
    }
}