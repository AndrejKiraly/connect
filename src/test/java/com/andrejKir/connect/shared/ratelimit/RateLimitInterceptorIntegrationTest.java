package com.andrejKir.connect.shared.ratelimit;

import com.andrejKir.connect.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class RateLimitInterceptorIntegrationTest extends AbstractIntegrationTest {

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

    private ResultActions login(String ip, String usernameOrEmail) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"" + usernameOrEmail + "\",\"password\":\"wrong-password\"}"));
    }
}