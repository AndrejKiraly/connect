package com.andrejKir.connect.messaging.controller;

import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserPrivateSummaryResponse;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private AppUserPrivateSummaryResponse ana;
    private AppUserPrivateSummaryResponse bob;
    private UUID conversationId;

    @Autowired
    TransactionTemplate transactionTemplate;

    @AfterEach
    void clearMessaging() {
        messageRepository.deleteAll();
        conversationMemberRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    @BeforeEach
    void seed() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ana = register("ana" + suffix);
        bob = register("bob" + suffix);

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
        AppUserPrivateSummaryResponse stranger = register("stranger" + UUID.randomUUID().toString().substring(0, 8));
        Cookie session = loginAs(stranger.username());

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
        UUID otherConversationId = openDirectConversation(bob.id(), register("cyril" + UUID.randomUUID().toString().substring(0, 8)).id());
        UUID foreignMessageId = persistMessage(otherConversationId, bob.id());

        markRead(conversationId, ana.id(), foreignMessageId);

        assertNull(watermarkOf(conversationId, ana.id()));
    }

    private void markRead(UUID conversationId, UUID appUserId, UUID messageId) {
        transactionTemplate.executeWithoutResult(
                status -> conversationMemberRepository.markRead(conversationId, appUserId, messageId));
    }

    private org.springframework.test.web.servlet.ResultActions sendMessage(Cookie session, UUID conversationId, String body)
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
        return messageRepository.save(Message.text(conversationId, senderId, "x")).getId();
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

    private AppUserPrivateSummaryResponse register(String username) {
        return appUserService.registerUser(new RegisterRequest(
                username + "@example.com", username, PASSWORD, username,
                "First", "Last", LocalDate.of(2000, 1, 1)));
    }
}