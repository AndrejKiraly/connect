package com.andrejKir.connect.social.controller;

import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserPrivateSummaryResponse;
import com.andrejKir.connect.accounts.entity.AppUser;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FriendshipIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "trombone-sunset-91";
    private static final String TEST_IP = "10.5.5.5";

    @MockitoSpyBean
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

    @Test
    void show_pendingByUser_returnsListWith401()throws Exception{
        Cookie sessionA = loginAs("userA");
        mockMvc.perform(get("/api/v1/friendship/friends").cookie(sessionA))
                .andExpect(status().isOk());
    }

    @Test
    void friends_returnsAccepted_excludesPendingAndDeclined() throws Exception {
        RequestGroup g = seedRequests(userA.id());
        Cookie session = loginAs("userA");

        mockMvc.perform(get("/api/v1/friendship/friends").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].counterpart.id",
                        containsInAnyOrder(g.friend1().toString(), g.friend2().toString())));
    }

    @Test
    void requestsIncoming_returnsRequestsToUser_excludesOutgoing() throws Exception {
        RequestGroup g = seedRequests(userA.id());
        Cookie session = loginAs("userA");

        mockMvc.perform(get("/api/v1/friendship/requests").param("direction", "incoming").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].counterpart.id",
                        containsInAnyOrder(g.inc1().toString(), g.inc2().toString())));
    }

    @Test
    void requestsOutgoing_returnsRequestsByUser_excludesIncoming() throws Exception {
        RequestGroup g = seedRequests(userA.id());
        Cookie session = loginAs("userA");

        mockMvc.perform(get("/api/v1/friendship/requests").param("direction", "outgoing").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].counterpart.id",
                        containsInAnyOrder(g.out1().toString(), g.out2().toString())));
    }

    @Test
    void friends_counterpartHasDisplayNameNotUsername() throws Exception {
        seedRequests(userA.id());
        Cookie session = loginAs("userA");

        mockMvc.perform(get("/api/v1/friendship/friends").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].counterpart.displayName").exists())
                .andExpect(jsonPath("$[0].counterpart.username").doesNotExist());
    }

    @Test
    void requests_invalidDirection_returns400() throws Exception {
        Cookie session = loginAs("userA");

        mockMvc.perform(get("/api/v1/friendship/requests").param("direction", "sideways").cookie(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requests_missingDirection_returns400() throws Exception {
        Cookie session = loginAs("userA");

        mockMvc.perform(get("/api/v1/friendship/requests").cookie(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void friends_whenNone_returnsEmptyList() throws Exception {
        Cookie session = loginAs("userA");

        mockMvc.perform(get("/api/v1/friendship/friends").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void friends_fetchesCounterpartsInSingleBatch() throws Exception {
        seedRequests(userA.id());
        Cookie session = loginAs("userA");

        mockMvc.perform(get("/api/v1/friendship/friends").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(appUserService, times(1)).getSummaries(anySet());
        verify(appUserService, never()).getSummary(any());
    }

    private RequestGroup seedRequests(UUID main) {
        AppUser friend1 = persistUser("friend1");
        AppUser friend2 = persistUser("friend2");
        AppUser inc1 = persistUser("inc1");
        AppUser inc2 = persistUser("inc2");
        AppUser out1 = persistUser("out1");
        AppUser out2 = persistUser("out2");
        AppUser dec1 = persistUser("dec1");
        AppUser dec2 = persistUser("dec2");

        acceptedFriendship(main, friend1.getId());
        acceptedFriendship(main, friend2.getId());
        seedPendingRequest(inc1.getId(), main);
        seedPendingRequest(inc2.getId(), main);
        seedPendingRequest(main, out1.getId());
        seedPendingRequest(main, out2.getId());
        declinedFriendship(main, dec1.getId());
        declinedFriendship(main, dec2.getId());

        return new RequestGroup(friend1.getId(), friend2.getId(), inc1.getId(), inc2.getId(), out1.getId(), out2.getId());
    }

    private record RequestGroup(UUID friend1, UUID friend2, UUID inc1, UUID inc2, UUID out1, UUID out2) {
    }

    private AppUserPrivateSummaryResponse register(String email, String username) {
        return appUserService.registerUser(new RegisterRequest(
                email, username, PASSWORD, username, "First", "Last", LocalDate.of(2000, 1, 1)));
    }

    private Friendship seedPendingRequest(UUID requesterId, UUID targetId) {
        return friendshipRepository.save(Friendship.request(UserPair.of(requesterId, targetId), requesterId));
    }

    private AppUser persistUser(String username) {
        return appUserRepository.save(new AppUser(
                username, username + "@example.com", "x", username, "First", "Last", LocalDate.of(2000, 1, 1)));
    }

    private void acceptedFriendship(UUID a, UUID b) {
        Friendship friendship = Friendship.request(UserPair.of(a, b), a);
        friendship.accept();
        friendshipRepository.save(friendship);
    }

    private void declinedFriendship(UUID a, UUID b) {
        Friendship friendship = Friendship.request(UserPair.of(a, b), a);
        friendship.decline();
        friendshipRepository.save(friendship);
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