package com.andrejKir.connect.accounts.enums;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public enum PostLifespan {
    DAY(Duration.ofDays(1)),
    WEEK(Duration.ofDays(7)),
    MONTH(Duration.ofDays(31)),
    FOREVER(null);

    private final Duration duration;


    PostLifespan(Duration duration) {
        this.duration = duration;
    }

    public Optional<Instant> expiryFrom (Instant createdAt) {
        return duration == null ? Optional.empty() : Optional.of(createdAt.plus(duration));
    }
}
