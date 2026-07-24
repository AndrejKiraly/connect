package com.andrejKir.connect.social.controller;

import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserPrivateSummaryResponse;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.shared.domain.UserPair;
import com.andrejKir.connect.social.entity.Friendship;
import com.andrejKir.connect.social.enums.FriendshipStatus;
import com.andrejKir.connect.social.repository.FriendshipRepository;
import com.andrejKir.connect.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FriendshipIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "trombone-sunset-91";
    private static final String TEST_IP = "10.5.5.5";

    @Autowired
    AppUserService appUserService;
    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    FriendshipRepository friendshipRepository;

    private AppUserPrivateSummaryResponse userA;
    private AppUserPrivateSummaryResponse userB;
    private AppUserPrivateSummaryResponse userC;

    @BeforeEach
    void seed() {
        friendshipRepository.deleteAll();
        appUserRepository.deleteAll();
        userA = register("a@example.com", "userA");
        userB = register("b@example.com", "userB");
        userC = register("c@example.com", "userC");
    }

    @Test
    void create_requesterIdInBody_isIgnored_actorIsAuthenticatedUser() throws Exception {
        Cookie sessionA = loginAs("userA");

        String body = "{\"targetId\":\"" + userB.id() + "\",\"requesterId\":\"" + userB.id() + "\"}";

        mockMvc.perform(post("/api/v1/friendship")
                        .with(csrfToken())
                        .cookie(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.counterpart.id").value(userB.id().toString()));

        Friendship created = friendshipRepository.findByUsers(UserPair.of(userA.id(), userB.id())).orElseThrow();
        assertEquals(userA.id(), created.getRequestedBy());
    }

    @Test
    void create_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/friendship")
                        .with(csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetId\":\"" + userB.id() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_unknownTarget_returns404() throws Exception {
        Cookie sessionA = loginAs("userA");

        mockMvc.perform(post("/api/v1/friendship")
                        .with(csrfToken())
                        .cookie(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void accept_byAddressee_persistsAccepted() throws Exception {
        Friendship pending = seedPendingRequest(userA.id(), userB.id());
        Cookie sessionB = loginAs("userB");

        mockMvc.perform(post("/api/v1/friendship/" + pending.getId() + "/accept")
                        .with(csrfToken())
                        .cookie(sessionB))
                .andExpect(status().isOk());

        Friendship reloaded = friendshipRepository.findById(pending.getId()).orElseThrow();
        assertEquals(FriendshipStatus.ACCEPTED, reloaded.getStatus());
    }

    @Test
    void accept_byRequester_returns403() throws Exception {
        Friendship pending = seedPendingRequest(userA.id(), userB.id());
        Cookie sessionA = loginAs("userA");

        mockMvc.perform(post("/api/v1/friendship/" + pending.getId() + "/accept")
                        .with(csrfToken())
                        .cookie(sessionA))
                .andExpect(status().isForbidden());
    }

    @Test
    void accept_byStranger_returns404() throws Exception {
        Friendship pending = seedPendingRequest(userA.id(), userB.id());
        Cookie sessionC = loginAs("userC");

        mockMvc.perform(post("/api/v1/friendship/" + pending.getId() + "/accept")
                        .with(csrfToken())
                        .cookie(sessionC))
                .andExpect(status().isNotFound());
    }

    @Test
    void accept_alreadyAccepted_returns409() throws Exception {
        Friendship accepted = Friendship.request(UserPair.of(userA.id(), userB.id()), userA.id());
        accepted.accept();
        friendshipRepository.save(accepted);
        Cookie sessionB = loginAs("userB");

        mockMvc.perform(post("/api/v1/friendship/" + accepted.getId() + "/accept")
                        .with(csrfToken())
                        .cookie(sessionB))
                .andExpect(status().isConflict());
    }

    @Test
    void decline_byAddressee_persistsDeclined() throws Exception {
        Friendship pending = seedPendingRequest(userA.id(), userB.id());
        Cookie sessionB = loginAs("userB");

        mockMvc.perform(post("/api/v1/friendship/" + pending.getId() + "/decline")
                        .with(csrfToken())
                        .cookie(sessionB))
                .andExpect(status().isOk());

        Friendship reloaded = friendshipRepository.findById(pending.getId()).orElseThrow();
        assertEquals(FriendshipStatus.DECLINED, reloaded.getStatus());
    }

    private AppUserPrivateSummaryResponse register(String email, String username) {
        return appUserService.registerUser(new RegisterRequest(
                email, username, PASSWORD, username, "First", "Last", LocalDate.of(2000, 1, 1)));
    }

    private Friendship seedPendingRequest(UUID requesterId, UUID targetId) {
        return friendshipRepository.save(Friendship.request(UserPair.of(requesterId, targetId), requesterId));
    }

    private Cookie loginAs(String username) throws Exception {
        Cookie session = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrfToken())
                        .with(fromIp(TEST_IP))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("SESSION");
        if (session == null) {
            throw new IllegalStateException("No SESSION cookie after login");
        }
        return session;
    }
}