package com.andrejKir.connect.shared.ratelimit;


import com.andrejKir.connect.shared.exception.RateLimitExceededException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;



@Component
public class RateLimitService {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final Map<RateLimitPolicy, Cache<String, Bucket>> buckets;

    public RateLimitService() {
        this(Ticker.systemTicker());
    }

    RateLimitService(Ticker ticker) {
        Map<RateLimitPolicy, Cache<String, Bucket>> byPolicy = new EnumMap<>(RateLimitPolicy.class);

        for (RateLimitPolicy policy : RateLimitPolicy.values()) {
            byPolicy.put(policy, Caffeine.newBuilder()
                    .expireAfterAccess(policy.window())
                    .ticker(ticker)
                    .build());
        }
        this.buckets = Map.copyOf(byPolicy);
    }

    public void check(RateLimitPolicy policy, String key) {
        ConsumptionProbe probe = buckets.get(policy)
                .get(key, ignored -> newBucket(policy))
                .tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return;
        }
        throw new RateLimitExceededException(Math.ceilDiv(probe.getNanosToWaitForRefill(),
                NANOS_PER_SECOND));
    }

    public void clearAll() {
        buckets.values().forEach(Cache::invalidateAll);
    }

    private Bucket newBucket(RateLimitPolicy policy) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(policy.capacity())
                        .refillGreedy(policy.capacity(), policy.window())
                        .build())
                .build();
    }
}