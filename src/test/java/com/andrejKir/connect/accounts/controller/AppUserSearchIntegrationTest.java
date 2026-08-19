package com.andrejKir.connect.accounts.controller;

import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserPrivateSummaryResponse;
import com.andrejKir.connect.accounts.entity.AppUserSettings;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
import com.andrejKir.connect.accounts.repository.AppUserSettingsRepository;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AppUserSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    AppUserService appUserService;
    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    AppUserSettingsRepository appUserSettingsRepository;

    private AppUserPrivateSummaryResponse lubo;
    private AppUserPrivateSummaryResponse kovalcik;
    private AppUserPrivateSummaryResponse anna;

    @BeforeEach
    void seed() {
        appUserRepository.deleteAll();
        register("searcher@example.com", "searcher", "Searcher Person");
        lubo = register("lubo@example.com", "luboander", "Ľubo Ander");
        kovalcik = register("kovalcik@example.com", "kovalcik", "Kovalčík Peter");
        anna = register("anna@example.com", "annak", "Anna Kováčová");
    }

    @Test
    void search_ignoresDiacriticsAndCase() throws Exception {
        search("LUBO").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(lubo.id().toString()))
                .andExpect(jsonPath("$[0].displayName").value("Ľubo Ander"));
    }

    @Test
    void search_ranksPrefixMatchesBeforeMatchesInsideName() throws Exception {
        search("ko").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(kovalcik.id().toString()))
                .andExpect(jsonPath("$[1].id").value(anna.id().toString()));
    }

    @Test
    void search_queryShorterThanTwoCharacters_returnsNothing() throws Exception {
        search("k").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void search_neverReturnsTheSearcher() throws Exception {
        search("searcher").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void search_excludesDeactivatedUser() throws Exception {
        appUserService.deactivate(lubo.id());

        search("lubo").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void search_excludesUserHiddenFromNameSearch() throws Exception {
        setDiscoverable(lubo.id(), false, true);

        search("lubo").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void search_doesNotExposeUsername() throws Exception {
        search("lubo").andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").doesNotExist());
    }

    @Test
    void search_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("q", "lubo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void code_findsUser() throws Exception {
        byCode(inviteCodeOf(lubo.id())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(lubo.id().toString()));
    }

    @Test
    void code_acceptsLowercaseAndSeparators() throws Exception {
        String inviteCode = inviteCodeOf(lubo.id());
        String typedByHand = inviteCode.substring(0, 4).toLowerCase() + "-" + inviteCode.substring(4).toLowerCase();

        byCode(typedByHand).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(lubo.id().toString()));
    }

    @Test
    void code_hiddenUserIsIndistinguishableFromUnknownCode() throws Exception {
        setDiscoverable(lubo.id(), true, false);

        String hidden = byCode(inviteCodeOf(lubo.id())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String unknown = byCode("ZZZZZZZZ").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(unknown, hidden);
    }

    @Test
    void findUsers_withoutAnyFilter_returns400() throws Exception {
        Cookie session = loginAs("searcher");

        mockMvc.perform(get("/api/v1/users").cookie(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findUsers_withBothFilters_returns400() throws Exception {
        Cookie session = loginAs("searcher");

        mockMvc.perform(get("/api/v1/users").param("q", "lubo").param("code", "ZZZZZZZZ").cookie(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profile_deactivatedUser_returnsTombstoneInsteadOfRealName() throws Exception {
        appUserService.deactivate(lubo.id());
        Cookie session = loginAs("searcher");

        mockMvc.perform(get("/api/v1/users/" + lubo.id()).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.deleted").value(true))
                .andExpect(jsonPath("$.user.displayName").value("Deleted user"))
                .andExpect(jsonPath("$.description").value(""));
    }

    @Test
    void profile_unknownId_returns404() throws Exception {
        Cookie session = loginAs("searcher");

        mockMvc.perform(get("/api/v1/users/" + UUID.randomUUID()).cookie(session))
                .andExpect(status().isNotFound());
    }

    private ResultActions search(String query) throws Exception {
        return mockMvc.perform(get("/api/v1/users").param("q", query).cookie(loginAs("searcher")));
    }

    private ResultActions byCode(String inviteCode) throws Exception {
        return mockMvc.perform(get("/api/v1/users").param("code", inviteCode).cookie(loginAs("searcher")));
    }

    private AppUserPrivateSummaryResponse register(String email, String username, String displayName) {
        return appUserService.registerUser(new RegisterRequest(
                email, username, PASSWORD, displayName, "First", "Last", LocalDate.of(2000, 1, 1)));
    }

    private String inviteCodeOf(UUID appUserId) {
        return appUserRepository.findById(appUserId).orElseThrow().getInviteCode();
    }

    private void setDiscoverable(UUID appUserId, boolean byName, boolean byCode) {
        AppUserSettings settings = appUserSettingsRepository.findById(appUserId).orElseThrow();
        settings.update(settings.getLanguage(), settings.getDefaultPostLifespan(), byName, byCode);
        appUserSettingsRepository.save(settings);
    }
}