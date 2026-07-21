package com.andrejKir.connect.shared.ratelimit;

import com.andrejKir.connect.shared.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RateLimitServiceTest {

    private final AtomicLong nanos = new AtomicLong();
    private final RateLimitService service = new RateLimitService(nanos::get);

    @Test
    void allowsAgain_afterKeyIdleForFullWindow() {
        drain(RateLimitPolicy.LOGIN_PER_USER, "victim");

        advance(RateLimitPolicy.LOGIN_PER_USER.window().plusSeconds(1));

        assertDoesNotThrow(() -> service.check(RateLimitPolicy.LOGIN_PER_USER, "victim"));
    }

    @Test
    void staysLimited_beforeIdleWindowElapses() {
        drain(RateLimitPolicy.LOGIN_PER_USER, "victim");

        advance(RateLimitPolicy.LOGIN_PER_USER.window().minusMinutes(1));

        assertThrows(RateLimitExceededException.class,
                () -> service.check(RateLimitPolicy.LOGIN_PER_USER, "victim"));
    }

    private void drain(RateLimitPolicy policy, String key) {
        for (long i = 0; i < policy.capacity(); i++) {
            service.check(policy, key);
        }
    }

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }
}
