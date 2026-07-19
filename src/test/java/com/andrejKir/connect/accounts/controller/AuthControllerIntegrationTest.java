package com.andrejKir.connect.accounts.controller;


import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String STRONG_PASSWORD = "trombone-sunset-91";

    private static int ipCounter = 0;

    @Autowired
    AppUserService appUserService;
    @Autowired
    AppUserRepository appUserRepository;

    @BeforeEach
    void seedKnownUser() {
        ipCounter++;
        appUserRepository.deleteAll();
        appUserService.registerUser(new RegisterRequest(
                "taken@example.com", "takenuser", STRONG_PASSWORD,
                "LuboAnder", "Lubo", "Ander", LocalDate.of(2000, 1, 1)));
    }

    @Test
    void login_unknownUsername_returns401() throws Exception {
        login("unknown", "wrongPassword").andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        login("unknown@gmail.com", "wrongPassword").andExpect(status().isUnauthorized());
    }

    @Test
    void login_wrongPasswordExistingUser_returns401() throws Exception {
        login("takenuser", "wrongPassword").andExpect(status().isUnauthorized());
    }

    @Test
    void register_newUser_returns201() throws Exception {
        register(new RegisterRequest("newuser@example.com", "newuser1", STRONG_PASSWORD,
                "NewUser", "Lubo", "Ander", LocalDate.of(2000, 1, 1)))
                .andExpect(status().isCreated());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        register(new RegisterRequest("taken@example.com", "newuser1", STRONG_PASSWORD,
                "NewUser", "Lubo", "Ander", LocalDate.of(2000, 1, 1)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        register(new RegisterRequest("newuser@example.com", "takenuser", STRONG_PASSWORD,
                "NewUser", "Lubo", "Ander", LocalDate.of(2000, 1, 1)))
                .andExpect(status().isConflict());
    }

    private ResultActions login(String usernameOrEmail, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .with(request -> { request.setRemoteAddr(testIp()); return request; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"" + usernameOrEmail + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions register(RegisterRequest request) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .with(request2 -> { request2.setRemoteAddr(testIp()); return request2; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private String testIp() {
        return "10.4.4." + ipCounter;
    }
}