package com.andrejKir.connect.accounts.controller;

import com.andrejKir.connect.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PasswordResetRateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "trombone-sunset-91";

    @Test
    void returns429_afterSixthForgotFromSameIp() throws Exception {
        String ip = "10.4.4.4";

        for (int i = 1; i <= 5; i++) {
            forgot(ip, "rl-forgot-ip-" + i + "@example.com").andExpect(status().isOk());
        }

        forgot(ip, "rl-forgot-ip-6@example.com")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void returns429_afterFourthForgotForSameEmail() throws Exception {
        String email = "rl-mailbomb@example.com";

        for (int i = 1; i <= 3; i++) {
            forgot("10.8.8." + i, email).andExpect(status().isOk());
        }

        forgot("10.8.8.4", email)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void returns429_afterEleventhResetFromSameIp() throws Exception {
        String ip = "10.6.6.6";

        for (int i = 1; i <= 10; i++) {
            reset(ip, "no-such-token-" + i).andExpect(status().isBadRequest());
        }

        reset(ip, "no-such-token-11")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    private ResultActions forgot(String ip, String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password/forgot")
                .with(csrfToken())
                .with(fromIp(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"));
    }

    private ResultActions reset(String ip, String resetToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password/reset")
                .with(csrfToken())
                .with(fromIp(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"" + PASSWORD + "\",\"resetToken\":\"" + resetToken + "\"}"));
    }

    private RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}