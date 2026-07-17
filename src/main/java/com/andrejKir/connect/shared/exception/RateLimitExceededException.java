package com.andrejKir.connect.shared.exception;

public class RateLimitExceededException extends LocalizedException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("error.rate-limit.exceeded", "Rate limit exceeded, retry after " + retryAfterSeconds + "s", retryAfterSeconds);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}