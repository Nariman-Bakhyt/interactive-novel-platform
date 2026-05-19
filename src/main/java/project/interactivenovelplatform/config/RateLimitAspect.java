package project.interactivenovelplatform.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import project.interactivenovelplatform.security.UserPrincipal;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final LettuceBasedProxyManager<byte[]> proxyManager;
    private final HttpServletRequest request;

    // Кэшируем конфигурации Bucket4j, чтобы не выделять память и не пересоздавать правила лимитирования на каждый входящий запрос.
    private final Map<String, BucketConfiguration> configCache = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimited)")
    public Object applyRateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        String rateLimitKey;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Разделяем лимиты для авторизованных пользователей (по user:<id>) и гостей (по guest:<id>), чтобы предотвратить взаимное влияние нагрузок.
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            
            rateLimitKey = "user:" + userPrincipal.getId();
        } else {
            
            String guestId = (String) request.getAttribute("VALID_GUEST_ID");
            if (guestId == null) {
                
                
                
                return joinPoint.proceed(); 
            }
            rateLimitKey = "guest:" + guestId;
        }

        
        String methodName = joinPoint.getSignature().toShortString();
        byte[] key = ("limit:" + methodName + ":" + rateLimitKey).getBytes(StandardCharsets.UTF_8);

        
        String configKey = rateLimited.capacity() + "-" + rateLimited.minutes();
        BucketConfiguration config = configCache.computeIfAbsent(configKey, k -> 
            BucketConfiguration.builder()
                    .addLimit(limit -> limit.capacity(rateLimited.capacity())
                            .refillGreedy(rateLimited.capacity(), Duration.ofMinutes(rateLimited.minutes())))
                    .build());

        
        Bucket bucket = proxyManager.getProxy(key, () -> config);
        if (bucket.tryConsume(1)) {
            return joinPoint.proceed(); 
        } else {
            log.warn("Rate limit exceeded for key: {}", new String(key, StandardCharsets.UTF_8));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Лимит запросов исчерпан");
        }
    }
}
