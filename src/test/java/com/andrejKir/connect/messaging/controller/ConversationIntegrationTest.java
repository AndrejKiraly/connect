package com.andrejKir.connect.messaging.controller;

import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserPrivateSummaryResponse;
import com.andrejKir.connect.accounts.service.AppUserService;
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
                .andExpect(jsonPath("$.length()").value(0));

        messageRepository.save(Message.text(conversationId, bob.id(), "ahoj"));

        mockMvc.perform(get("/api/v1/conversations").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(conversationId.toString()));
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
        String username = prefix + UUID.randomUUID().toString().substring(0, 8);
        return appUserService.registerUser(new RegisterRequest(
                username + "@example.com", username, PASSWORD, username,
                "First", "Last", LocalDate.of(2000, 1, 1)));
    }
}
