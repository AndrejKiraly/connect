package com.andrejKir.connect.shared.ratelimit;


import com.andrejKir.connect.shared.web.ApiPaths;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Map<String, RateLimitPolicy> POLICIES_BY_PATH = Map.of(
            ApiPaths.V1 + "/auth/login", RateLimitPolicy.LOGIN_PER_IP,
            ApiPaths.V1 + "/auth/register", RateLimitPolicy.REGISTER_PER_IP,
            ApiPaths.V1 + "/auth/password/forgot", RateLimitPolicy.PASSWORD_FORGOT_PER_IP,
            ApiPaths.V1 + "/auth/password/reset", RateLimitPolicy.PASSWORD_RESET_PER_IP);

    private final RateLimitService rateLimitService;


    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        RateLimitPolicy policy = POLICIES_BY_PATH.get(request.getRequestURI());
        if (policy != null) {
            rateLimitService.check(policy, request.getRemoteAddr());
        }
        return true;
    }
}