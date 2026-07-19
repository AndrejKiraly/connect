package com.andrejKir.connect.shared.security;


import com.andrejKir.connect.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CsrfIntegrationTest extends AbstractIntegrationTest {

    @Test
    void get_setsXsrfTokenCookie() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void post_WithCookieButNoHeader_returns403() throws Exception {
        Cookie xsrf = fetchXsrfCookie();

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(xsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"csrf-a\",\"password\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void post_WithCookieAndRawHeader_passesCsrf() throws Exception {
        Cookie xsrf = fetchXsrfCookie();

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"csrf-b\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    private Cookie fetchXsrfCookie() throws Exception {
        return mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
    }
}