package com.andrejKir.connect.shared.ratelimit;


import com.andrejKir.connect.shared.exception.RateLimitExceededException;
import com.andrejKir.connect.shared.web.ApiPaths;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String LOGIN_PATH = ApiPaths.V1 + "/auth/login";
    private static final String REGISTER_PATH = ApiPaths.V1 + "/auth/register";

    private final RateLimitService rateLimitService;


    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String ip = request.getRemoteAddr();

        ConsumptionProbe probe;
        if (LOGIN_PATH.equals(path)) {
            probe = rateLimitService.tryLoginByIp(ip);
        }else if(REGISTER_PATH.equals(path)){
            probe = rateLimitService.tryRegisterByIp(ip);
        }else {
            return true;
        }
        if (probe.isConsumed()){
            return true;
        }
        long retryAfterSeconds = Math.ceilDiv(probe.getNanosToWaitForRefill(), 1_000_000_000L);
        throw new RateLimitExceededException(retryAfterSeconds);
    }
}
