package com.andrejKir.connect.accounts.controller;

import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.net.URI;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthRateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "trombone-sunset-91";
    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String ENCODED_REGISTER_PATH = "/api/v1/auth/%72egister";

    @Test
    void returns429_afterFifthLoginForSameUsername() throws Exception {
        String ip = "10.1.1.1";
        String username = "rl-name-user";

        for (int i = 0; i < 5; i++) {
            login(ip, username).andExpect(status().isUnauthorized());
        }

        login(ip, username)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void returns429_afterTwentiethLoginFromSameIp() throws Exception {
        String ip = "10.2.2.2";

        for (int i = 1; i <= 20; i++) {
            login(ip, "ip-user-" + i).andExpect(status().isUnauthorized());
        }

        login(ip, "ip-user-21")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void returns429_afterEleventhRegisterFromSameIp() throws Exception {
        String ip = "10.9.9.9";

        for (int i = 1; i <= 10; i++) {
            register(ip, REGISTER_PATH, "rl-reg-" + i).andExpect(status().isCreated());
        }

        register(ip, REGISTER_PATH, "rl-reg-11")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void returns429_afterEleventhRegisterFromSameIp_viaPercentEncodedPath() throws Exception {
        String ip = "10.9.9.10";

        for (int i = 1; i <= 10; i++) {
            register(ip, REGISTER_PATH, "rl-enc-" + i).andExpect(status().isCreated());
        }

        register(ip, ENCODED_REGISTER_PATH, "rl-enc-11")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    private ResultActions login(String ip, String usernameOrEmail) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .with(csrfToken())
                .with(fromIp(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"" + usernameOrEmail + "\",\"password\":\"wrong-password\"}"));
    }

    private ResultActions register(String ip, String path, String username) throws Exception {
        RegisterRequest request = new RegisterRequest(
                username + "@example.com", username, PASSWORD,
                "RateLimit", "Rate", "Limit", LocalDate.of(2000, 1, 1));
        return mockMvc.perform(post(URI.create(path))
                .with(csrfToken())
                .with(fromIp(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}