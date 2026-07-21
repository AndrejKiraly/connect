package com.andrejKir.connect.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class AbsoluteSessionTimeoutFilter extends OncePerRequestFilter {

    private final Duration absoluteTimeout;
    private final Clock clock;

    public AbsoluteSessionTimeoutFilter(Duration absoluteTimeout, Clock clock) {
        this.absoluteTimeout = absoluteTimeout;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && hasReachedAbsoluteTimeout(session)) {
            session.invalidate();
        }
        filterChain.doFilter(request, response);
    }

    private boolean hasReachedAbsoluteTimeout(HttpSession session) {
        Instant expiresAt = Instant.ofEpochMilli(session.getCreationTime()).plus(absoluteTimeout);
        return !clock.instant().isBefore(expiresAt);
    }
}