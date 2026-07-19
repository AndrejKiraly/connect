package com.andrejKir.connect.accounts.controller;


import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.exception.DuplicateEmailException;
import com.andrejKir.connect.accounts.exception.DuplicateUserException;
import com.andrejKir.connect.accounts.exception.DuplicateUsernameException;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String STRONG_PASSWORD = "trombone-sunset-91";
    private static final String BLOCKLISTED_PASSWORD = "Password123";
    private static final String TOO_SHORT_PASSWORD = "Qx7!aB9";

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

    @Test
    void register_blocklistedPassword_returns400() throws Exception {
        register(new RegisterRequest("weakpass@example.com", "weakpass1", BLOCKLISTED_PASSWORD,
                "NewUser", "Lubo", "Ander", LocalDate.of(2000, 1, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void register_tooShortPassword_returns400() throws Exception {
        register(new RegisterRequest("shortpass@example.com", "shortpass1", TOO_SHORT_PASSWORD,
                "NewUser", "Lubo", "Ander", LocalDate.of(2000, 1, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void registerUser_concurrentRequestsWithSameEmail_succeedOnlyOnce() throws Exception {
        RegisterRequest request = new RegisterRequest("race@example.com", "raceuser", STRONG_PASSWORD,
                "RaceUser", "Race", "User", LocalDate.of(2000, 1, 1));

        int concurrentAttempts = 2;
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentAttempts);
        List<Future<Boolean>> attempts = new ArrayList<>();

        for (int i = 0; i < concurrentAttempts; i++) {
            attempts.add(executor.submit(() -> {
                startGate.await();
                try {
                    appUserService.registerUser(request);
                    return true;
                } catch (DuplicateUserException | DuplicateEmailException | DuplicateUsernameException e) {
                    return false;
                }
            }));
        }
        startGate.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        long succeeded = 0;
        for (Future<Boolean> attempt : attempts) {
            if (attempt.get()) {
                succeeded++;
            }
        }
        assertEquals(1, succeeded);
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