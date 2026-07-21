package com.andrejKir.connect.shared.ratelimit;


import com.andrejKir.connect.shared.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



@Component
public class RateLimitService {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final Map<RateLimitPolicy, Map<String, Bucket>> buckets;

    public RateLimitService() {
        Map<RateLimitPolicy, Map<String, Bucket>> byPolicy = new EnumMap<>(RateLimitPolicy.class);

        for (RateLimitPolicy policy : RateLimitPolicy.values()) {
            byPolicy.put(policy, new ConcurrentHashMap<>());
        }
        this.buckets = Map.copyOf(byPolicy);
    }

    public void check(RateLimitPolicy policy, String key) {
        ConsumptionProbe probe = buckets.get(policy)
                .computeIfAbsent(key, ignored -> newBucket(policy))
                .tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return;
        }
        throw new RateLimitExceededException(Math.ceilDiv(probe.getNanosToWaitForRefill(),
                NANOS_PER_SECOND));
    }

    public void clearAll() {
        buckets.values().forEach(Map::clear);
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