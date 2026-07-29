package com.andrejKir.connect.shared.ratelimit;

import java.time.Duration;

public enum RateLimitPolicy {
    LOGIN_PER_IP(20, Duration.ofMinutes(15)),
    LOGIN_PER_USER(5, Duration.ofMinutes(15)),
    REGISTER_PER_IP(10, Duration.ofHours(1)),
    PASSWORD_FORGOT_PER_IP(5, Duration.ofHours(1)),
    PASSWORD_FORGOT_PER_EMAIL(3, Duration.ofHours(1)),
    PASSWORD_RESET_PER_IP(10, Duration.ofHours(1)),
    FRIENDSHIP_REQUEST_PER_USER(110, Duration.ofHours(1)),
    MESSAGE_SEND_BURST_PER_USER(60, Duration.ofMinutes(1)),
    MESSAGE_SEND_PER_USER(1500, Duration.ofHours(12));

    private final long capacity;
    private final Duration window;

    RateLimitPolicy(long capacity, Duration window) {
        this.capacity = capacity;
        this.window = window;
    }

    public long capacity() {
        return capacity;
    }

    public Duration window() {
        return window;
    }
}
