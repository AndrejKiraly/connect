package com.andrejKir.connect.accounts.controller;

import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.dto.response.AppUserPrivateSummaryResponse;
import com.andrejKir.connect.accounts.entity.AppUser;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AppUserDeactivationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    AppUserService appUserService;

    private String username;
    private AppUserPrivateSummaryResponse user;

    @BeforeEach
    void seed() {
        username = "deact" + UUID.randomUUID().toString().substring(0, 8);
        user = appUserService.registerUser(new RegisterRequest(
                username + "@example.com", username, PASSWORD, username,
                "First", "Last", LocalDate.of(2000, 1, 1)));
    }

    @Test
    void deleteMe_marksAccountDeactivated_andKeepsTheRow() throws Exception {
        Cookie session = loginAs(username);

        mockMvc.perform(delete("/api/v1/users/me").with(csrfToken()).cookie(session))
                .andExpect(status().isNoContent());

        AppUser reloaded = appUserRepository.findById(user.id()).orElseThrow();
        assertNotNull(reloaded.getDeactivatedAt());
        assertTrue(reloaded.isDeactivated());
    }

    @Test
    void deleteMe_invalidatesSession() throws Exception {
        Cookie session = loginAs(username);

        mockMvc.perform(delete("/api/v1/users/me").with(csrfToken()).cookie(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me").cookie(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me").with(csrfToken()))
                .andExpect(status().isUnauthorized());
    }
}