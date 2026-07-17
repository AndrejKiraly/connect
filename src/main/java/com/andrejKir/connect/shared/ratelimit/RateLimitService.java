package com.andrejKir.connect.shared.ratelimit;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



@Component
public class RateLimitService {

    private static final RateLimitPolicy LOGIN_PER_IP = new RateLimitPolicy(20, Duration.ofMinutes(15));
    private static final RateLimitPolicy LOGIN_PER_USER = new RateLimitPolicy(5, Duration.ofMinutes(15));
    private static final RateLimitPolicy REGISTER_PER_IP = new RateLimitPolicy(5, Duration.ofHours(1));

    private final Map<String, Bucket> loginIpBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerIpBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> loginUserBuckets = new ConcurrentHashMap<>();



    public ConsumptionProbe tryLoginByIp(String ip) {
        return loginIpBuckets.computeIfAbsent(ip, key -> newBucket(LOGIN_PER_IP))
                .tryConsumeAndReturnRemaining(1);
    }

    public ConsumptionProbe tryRegisterByIp(String ip){
        return registerIpBuckets.computeIfAbsent(ip, key -> newBucket(REGISTER_PER_IP))
                .tryConsumeAndReturnRemaining(1);
    }

    public ConsumptionProbe tryLoginByUser(String usernameOrEmail){
        return loginUserBuckets.computeIfAbsent(usernameOrEmail, key -> newBucket(LOGIN_PER_USER))
                .tryConsumeAndReturnRemaining(1);
    }


    private Bucket newBucket(RateLimitPolicy policy) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(policy.capacity())
                        .refillGreedy(policy.capacity(), policy.window())
                        .build())
                .build();
    }

    private record RateLimitPolicy(long capacity, Duration window) {}

}