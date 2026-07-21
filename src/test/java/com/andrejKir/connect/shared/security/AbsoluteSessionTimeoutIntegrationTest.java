package com.andrejKir.connect.shared.security;

import com.andrejKir.connect.accounts.dto.request.RegisterRequest;
import com.andrejKir.connect.accounts.repository.AppUserRepository;
import com.andrejKir.connect.accounts.service.AppUserService;
import com.andrejKir.connect.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AbsoluteSessionTimeoutIntegrationTest extends AbstractIntegrationTest {

    private static final String USERNAME = "absolutetimeoutuser";
    private static final String PASSWORD = "trombone-sunset-91";
    private static final String TEST_IP = "10.7.7.7";

    @MockitoBean
    Clock clock;

    @Autowired
    AppUserService appUserService;
    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    CookieSerializer cookieSerializer;

    private Instant loginTime;

    @BeforeEach
    void seedUser() {
        loginTime = Instant.now();
        when(clock.instant()).thenReturn(loginTime);

        appUserRepository.deleteAll();
        appUserService.registerUser(new RegisterRequest(
                "absolute-timeout@example.com", USERNAME, PASSWORD,
                "Absolute", "Abs", "Timeout", LocalDate.of(2000, 1, 1)));
    }

    @Test
    void sessionStillWorks_justBeforeAbsoluteTimeout() throws Exception {
        Cookie session = login();

        when(clock.instant()).thenReturn(loginTime.plus(Duration.ofDays(89)));

        mockMvc.perform(get("/api/v1/users/me").cookie(session))
                .andExpect(status().isOk());
    }

    @Test
    void sessionIsRejected_afterAbsoluteTimeout() throws Exception {
        Cookie session = login();

        when(clock.instant()).thenReturn(loginTime.plus(Duration.ofDays(91)));

        mockMvc.perform(get("/api/v1/users/me").cookie(session))
                .andExpect(status().isForbidden());
    }

    private Cookie login() throws Exception {
        Cookie session = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrfToken())
                        .with(request -> {
                            request.setRemoteAddr(TEST_IP);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(sessionCookieName());

        assertNotNull(session);
        return session;
    }

    private String sessionCookieName() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        cookieSerializer.writeCookieValue(
                new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, "probe"));
        return response.getCookies()[0].getName();
    }
}