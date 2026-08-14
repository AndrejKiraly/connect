package com.andrejKir.connect.messaging.controller;

import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserPrivateSummaryResponse;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.messaging.entity.Conversation;
import com.andrejKir.connect.messaging.entity.ConversationMember;
import com.andrejKir.connect.messaging.entity.ConversationMemberId;
import com.andrejKir.connect.messaging.entity.Message;
import com.andrejKir.connect.messaging.repository.ConversationMemberRepository;
import com.andrejKir.connect.messaging.repository.ConversationRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ConversationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    AppUserService appUserService;
    @Autowired
    FriendshipRepository friendshipRepository;
    @Autowired
    ConversationRepository conversationRepository;
    @Autowired
    ConversationMemberRepository conversationMemberRepository;
    @Autowired
    MessageRepository messageRepository;

    private AppUserPrivateSummaryResponse ana;
    private AppUserPrivateSummaryResponse bob;

    @AfterEach
    void clearMessaging() {
        messageRepository.deleteAllInBatch();
        conversationMemberRepository.deleteAllInBatch();
        conversationRepository.deleteAllInBatch();
    }

    @BeforeEach
    void seed() {
        ana = register("ana");
        bob = register("bob");
        befriend(ana.id(), bob.id());
    }

    @Test
    void createConversation_createsDirectConversationWithBothMembers() throws Exception {
        UUID conversationId = idOf(createConversation(loginAs(ana.username()), bob.id())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DIRECT"))
                .andExpect(jsonPath("$.counterpart.id").value(bob.id().toString()))
                .andExpect(jsonPath("$.counterpart.displayName").value(bob.username())));

        assertTrue(conversationMemberRepository.existsById(new ConversationMemberId(conversationId, ana.id())));
        assertTrue(conversationMemberRepository.existsById(new ConversationMemberId(conversationId, bob.id())));
    }

    @Test
    void createConversation_fromCounterpartSide_returnsTheSameConversation() throws Exception {
        UUID openedByAna = idOf(createConversation(loginAs(ana.username()), bob.id())
                .andExpect(status().isCreated()));

        UUID openedByBob = idOf(createConversation(loginAs(bob.username()), ana.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counterpart.id").value(ana.id().toString())));

        assertEquals(openedByAna, openedByBob);
        assertEquals(1, conversationRepository.count());
    }

    @Test
    void createConversation_withNonFriend_returns403() throws Exception {
        AppUserPrivateSummaryResponse stranger = register("stranger");

        createConversation(loginAs(ana.username()), stranger.id())
                .andExpect(status().isForbidden());

        assertEquals(0, conversationRepository.count());
    }

    @Test
    void createConversation_withSelf_returns400() throws Exception {
        createConversation(loginAs(ana.username()), ana.id())
                .andExpect(status().isBadRequest());

        assertEquals(0, conversationRepository.count());
    }

    @Test
    void createdConversation_staysOutOfInboxUntilFirstMessage() throws Exception {
        Cookie session = loginAs(ana.username());
        UUID conversationId = idOf(createConversation(session, bob.id()).andExpect(status().isCreated()));

        mockMvc.perform(get("/api/v1/conversations").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations.length()").value(0));

        messageRepository.save(Message.text(conversationId, bob.id(), "ahoj"));

        mockMvc.perform(get("/api/v1/conversations").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations.length()").value(1))
                .andExpect(jsonPath("$.conversations[0].id").value(conversationId.toString()));
    }

    @Test
    void inbox_search_ignoresCaseAndDiacritics() throws Exception {
        Cookie session = loginAs(ana.username());
        UUID withLubo = seedConversationWith(register("lubo", "Ľubo Ander"), "ahoj").getConversationId();
        seedConversationWith(register("mira", "Mira Vlk"), "cau");

        mockMvc.perform(get("/api/v1/conversations").param("q", "lubo").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations.length()").value(1))
                .andExpect(jsonPath("$.conversations[0].id").value(withLubo.toString()));

        mockMvc.perform(get("/api/v1/conversations").param("q", "ANDER").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations.length()").value(1))
                .andExpect(jsonPath("$.conversations[0].id").value(withLubo.toString()));
    }

    @Test
    void inbox_search_treatsPercentAsLiteral() throws Exception {
        Cookie session = loginAs(ana.username());
        UUID withPercent = seedConversationWith(register("pct", "Lubo 50% Ander"), "ahoj").getConversationId();
        seedConversationWith(register("pcx", "Lubo 50x Ander"), "cau");

        mockMvc.perform(get("/api/v1/conversations").param("q", "50%").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations.length()").value(1))
                .andExpect(jsonPath("$.conversations[0].id").value(withPercent.toString()));
    }

    @Test
    void inbox_search_doesNotMatchDeactivatedUserByFormerName() throws Exception {
        Cookie session = loginAs(ana.username());
        AppUserPrivateSummaryResponse lubo = register("lubo", "Ľubo Ander");
        UUID conversationId = seedConversationWith(lubo, "ahoj").getConversationId();
        appUserService.deactivate(lubo.id());

        mockMvc.perform(get("/api/v1/conversations").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations.length()").value(1))
                .andExpect(jsonPath("$.conversations[0].id").value(conversationId.toString()))
                .andExpect(jsonPath("$.conversations[0].counterpart.deleted").value(true));

        mockMvc.perform(get("/api/v1/conversations").param("q", "Ander").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations.length()").value(0));
    }

    @Test
    void inbox_unreadFilter_returnsOnlyUnread() throws Exception {
        Cookie session = loginAs(ana.username());
        Message alreadyRead = seedConversationWith(register("dana"), "prva");
        UUID stillUnread = seedConversationWith(register("emil"), "druha").getConversationId();

        mockMvc.perform(post("/api/v1/conversations/" + alreadyRead.getConversationId() + "/read")
                        .with(csrfToken())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastReadMessageId\":\"" + alreadyRead.getId() + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/conversations").param("unread", "true").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations.length()").value(1))
                .andExpect(jsonPath("$.conversations[0].id").value(stillUnread.toString()));
    }

    @Test
    void inbox_withCursor_returnsOlderConversationsOnly() throws Exception {
        Cookie session = loginAs(ana.username());
        UUID oldest = seedConversationWith(register("dana"), "prva").getConversationId();
        seedConversationWith(register("emil"), "druha");
        seedConversationWith(register("fero"), "tretia");

        String body = mockMvc.perform(get("/api/v1/conversations").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations.length()").value(3))
                .andReturn().getResponse().getContentAsString();

        String cursor = objectMapper.readTree(body)
                .get("conversations").get(1).get("lastMessage").get("id").asText();

        mockMvc.perform(get("/api/v1/conversations").param("cursor", cursor).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversations.length()").value(1))
                .andExpect(jsonPath("$.conversations[0].id").value(oldest.toString()));
    }

    private ResultActions createConversation(Cookie session, UUID counterpartId) throws Exception {
        return mockMvc.perform(post("/api/v1/conversations")
                .with(csrfToken())
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"counterpartId\":\"" + counterpartId + "\"}"));
    }

    private UUID idOf(ResultActions response) throws Exception {
        String body = response.andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private void befriend(UUID a, UUID b) {
        Friendship friendship = Friendship.request(UserPair.of(a, b), a);
        friendship.accept();
        friendshipRepository.save(friendship);
    }

    private AppUserPrivateSummaryResponse register(String prefix) {
        return register(prefix, null);
    }

    private AppUserPrivateSummaryResponse register(String prefix, String displayName) {
        String username = prefix + UUID.randomUUID().toString().substring(0, 8);
        return appUserService.registerUser(new RegisterRequest(
                username + "@example.com", username, PASSWORD,
                displayName == null ? username : displayName,
                "First", "Last", LocalDate.of(2000, 1, 1)));
    }

    private Message seedConversationWith(AppUserPrivateSummaryResponse counterpart, String body) {
        Conversation conversation = conversationRepository.save(
                Conversation.direct(UserPair.of(ana.id(), counterpart.id())));
        conversationMemberRepository.saveAll(List.of(
                new ConversationMember(conversation.getId(), ana.id()),
                new ConversationMember(conversation.getId(), counterpart.id())));
        return messageRepository.save(Message.text(conversation.getId(), counterpart.id(), body));
    }
}
