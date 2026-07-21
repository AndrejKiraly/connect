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
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SessionFixationIntegrationTest extends AbstractIntegrationTest {

    private static final String USERNAME = "fixationuser";
    private static final String PASSWORD = "trombone-sunset-91";
    private static final String TEST_IP = "10.3.3.3";

    @Autowired
    SessionRepository<? extends Session> sessionRepository;
    @Autowired
    CookieSerializer cookieSerializer;
    @Autowired
    AppUserService appUserService;
    @Autowired
    AppUserRepository appUserRepository;

    @BeforeEach
    void seedUser() {
        appUserRepository.deleteAll();
        appUserService.registerUser(new RegisterRequest(
                "fixation@example.com", USERNAME, PASSWORD,
                "Fixation", "Fixa", "User", LocalDate.of(2000, 1, 1)));
    }

    @Test
    void login_withPreExistingSession_rotatesSessionIdAndKillsTheOldOne() throws Exception {
        String plantedSessionId = createPersistedSession(sessionRepository);
        Cookie plantedCookie = sessionCookie(plantedSessionId);

        Cookie rotatedCookie = login(plantedCookie)
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(plantedCookie.getName());

        assertNotNull(rotatedCookie);
        assertNotEquals(plantedSessionId, sessionIdOf(rotatedCookie));
        assertNull(sessionRepository.findById(plantedSessionId));

        mockMvc.perform(get("/api/v1/users/me").cookie(plantedCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_withPreExistingSession_rotatedSessionIsAuthenticated() throws Exception {
        Cookie plantedCookie = sessionCookie(createPersistedSession(sessionRepository));

        Cookie rotatedCookie = login(plantedCookie)
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(plantedCookie.getName());

        assertNotNull(rotatedCookie);
        mockMvc.perform(get("/api/v1/users/me").cookie(rotatedCookie))
                .andExpect(status().isOk());
    }

    private ResultActions login(Cookie sessionCookie) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .with(csrfToken())
                .with(request -> {
                    request.setRemoteAddr(TEST_IP);
                    return request;
                })
                .cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"));
    }

    private <S extends Session> String createPersistedSession(SessionRepository<S> repository) {
        S session = repository.createSession();
        repository.save(session);
        return session.getId();
    }

    private Cookie sessionCookie(String sessionId) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        cookieSerializer.writeCookieValue(
                new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, sessionId));
        return response.getCookies()[0];
    }

    private String sessionIdOf(Cookie sessionCookie) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(sessionCookie);
        return cookieSerializer.readCookieValues(request).getFirst();
    }
}