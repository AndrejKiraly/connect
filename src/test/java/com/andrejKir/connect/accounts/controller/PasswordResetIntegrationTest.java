package com.andrejKir.connect.accounts.controller;

import com.andrejKir.connect.accounts.dto.request.ForgotPasswordRequest;
import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.dto.request.ResetPasswordRequest;
import com.andrejKir.connect.accounts.entity.PasswordResetToken;
import com.andrejKir.connect.accounts.exception.InvalidPasswordResetTokenException;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
import com.andrejKir.connect.accounts.repository.PasswordResetTokenRepository;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.accounts.service.PasswordResetService;
import com.andrejKir.connect.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PasswordResetIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "reset-user@example.com";
    private static final String USERNAME = "resetuser";
    private static final String OLD_PASSWORD = "trombone-sunset-91";
    private static final String NEW_PASSWORD = "lantern-brook-47";
    private static final String UNKNOWN_EMAIL = "nobody@example.com";
    private static final String TEST_IP = "10.5.5.1";

    @Autowired
    AppUserService appUserService;
    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    JdbcIndexedSessionRepository sessionRepository;
    @Autowired
    PasswordResetService passwordResetService;

    @BeforeEach
    void seedKnownUser() {
        sessionRepository.findByPrincipalName(USERNAME).keySet().forEach(sessionRepository::deleteById);
        passwordResetTokenRepository.deleteAll();
        appUserRepository.deleteAll();
        appUserService.registerUser(new RegisterRequest(
                EMAIL, USERNAME, OLD_PASSWORD,
                "ResetUser", "Reset", "User", LocalDate.of(2000, 1, 1)));
    }

    @Test
    void resetPassword_validToken_replacesOldPassword() throws Exception {
        forgotPassword(EMAIL).andExpect(status().isOk());

        resetPassword(captureEmailedToken(), NEW_PASSWORD).andExpect(status().isNoContent());

        login(USERNAME, NEW_PASSWORD).andExpect(status().isOk());
        login(USERNAME, OLD_PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void resetPassword_alreadyUsedToken_returns400() throws Exception {
        forgotPassword(EMAIL).andExpect(status().isOk());
        String resetToken = captureEmailedToken();

        resetPassword(resetToken, NEW_PASSWORD).andExpect(status().isNoContent());

        resetPassword(resetToken, NEW_PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_unknownToken_returns400() throws Exception {
        resetPassword("this-token-was-never-issued", NEW_PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_expiredToken_returns400() throws Exception {
        String resetToken = "expired-but-otherwise-valid-token";
        UUID userId = appUserRepository.findByEmail(EMAIL).orElseThrow().getId();
        passwordResetTokenRepository.save(new PasswordResetToken(
                userId, sha256Hex(resetToken), Instant.now().minus(1, ChronoUnit.MINUTES)));

        resetPassword(resetToken, NEW_PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void forgotPassword_unknownEmail_returns200AndSendsNoMail() throws Exception {
        forgotPassword(UNKNOWN_EMAIL).andExpect(status().isOk());

        verify(passwordResetMailer, never()).sendResetLink(any(), any());
    }

    @Test
    void forgotPassword_knownEmail_returns200AndSendsMail() throws Exception {
        forgotPassword(EMAIL).andExpect(status().isOk());

        verify(passwordResetMailer).sendResetLink(eq(EMAIL), any());
    }

    @Test
    void resetPassword_validToken_revokesAllSessions() throws Exception {
        login(USERNAME, OLD_PASSWORD).andExpect(status().isOk());
        assertEquals(1, sessionRepository.findByPrincipalName(USERNAME).size());

        forgotPassword(EMAIL).andExpect(status().isOk());
        resetPassword(captureEmailedToken(), NEW_PASSWORD).andExpect(status().isNoContent());

        assertTrue(sessionRepository.findByPrincipalName(USERNAME).isEmpty());
    }

    @Test
    void resetPassword_validToken_invalidatesOtherOutstandingTokens() throws Exception {
        forgotPassword(EMAIL).andExpect(status().isOk());
        forgotPassword(EMAIL).andExpect(status().isOk());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetMailer, times(2)).sendResetLink(eq(EMAIL), tokenCaptor.capture());
        List<String> issuedTokens = tokenCaptor.getAllValues();

        resetPassword(issuedTokens.get(1), NEW_PASSWORD).andExpect(status().isNoContent());

        resetPassword(issuedTokens.get(0), NEW_PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_concurrentRequestsWithSameToken_succeedOnlyOnce() throws Exception {
        passwordResetService.generatePasswordResetToken(EMAIL);
        String resetToken = captureEmailedToken();

        int concurrentAttempts = 2;
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentAttempts);
        List<Future<Boolean>> attempts = new ArrayList<>();

        for (int i = 0; i < concurrentAttempts; i++) {
            attempts.add(executor.submit(() -> {
                startGate.await();
                try {
                    passwordResetService.resetPassword(resetToken, NEW_PASSWORD);
                    return true;
                } catch (InvalidPasswordResetTokenException e) {
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

    private static String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String captureEmailedToken() {
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetMailer).sendResetLink(eq(EMAIL), tokenCaptor.capture());
        return tokenCaptor.getValue();
    }

    private ResultActions forgotPassword(String email) throws Exception {
        return perform("/api/v1/auth/password/forgot", new ForgotPasswordRequest(email));
    }

    private ResultActions resetPassword(String resetToken, String newPassword) throws Exception {
        return perform("/api/v1/auth/password/reset", new ResetPasswordRequest(newPassword, resetToken));
    }

    private ResultActions login(String usernameOrEmail, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .with(csrfToken())
                .with(request -> { request.setRemoteAddr(TEST_IP); return request; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"" + usernameOrEmail + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions perform(String path, Object body) throws Exception {
        return mockMvc.perform(post(path)
                .with(csrfToken())
                .with(request -> { request.setRemoteAddr(TEST_IP); return request; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }
}