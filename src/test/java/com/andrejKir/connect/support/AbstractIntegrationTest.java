package com.andrejKir.connect.support;

import com.andrejKir.connect.accounts.service.PasswordResetMailer;
import com.andrejKir.connect.shared.ratelimit.RateLimitService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@ImportTestcontainers(SharedContainers.class)
public abstract class AbstractIntegrationTest {

    protected static final String CSRF_COOKIE = "XSRF-TOKEN";
    protected static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JsonMapper objectMapper;


    @MockitoBean
    protected PasswordResetMailer passwordResetMailer;

    @Autowired
    private RateLimitService rateLimitService;

    @BeforeEach
    void clearRateLimitBuckets() {
        rateLimitService.clearAll();
    }

    protected RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    protected RequestPostProcessor csrfToken() throws Exception {
        Cookie token = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getCookie(CSRF_COOKIE);
        if (token == null) {
            throw new IllegalStateException("No " + CSRF_COOKIE + " cookie was issued");
        }
        return request -> {
            List<Cookie> cookies = new ArrayList<>();
            if (request.getCookies() != null) {
                cookies.addAll(List.of(request.getCookies()));
            }
            cookies.add(token);
            request.setCookies(cookies.toArray(new Cookie[0]));
            request.addHeader(CSRF_HEADER, token.getValue());
            return request;
        };
    }
}
