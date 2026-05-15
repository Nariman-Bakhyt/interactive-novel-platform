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

    private final Map<String, BucketConfiguration> configCache = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimited)")
    public Object applyRateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        String rateLimitKey;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            // Пользователь аутентифицирован, используем его ID
            rateLimitKey = "user:" + userPrincipal.getId();
        } else {
            // Пользователь не аутентифицирован, используем ID гостя
            String guestId = (String) request.getAttribute("VALID_GUEST_ID");
            if (guestId == null) {
                // Это может произойти, если GuestIdFilter был пропущен или не смог установить ID.
                // В зависимости от требований, можно пропустить rate limiting или бросить ошибку.
                // Для простоты, пока пропускаем, но это место для потенциального улучшения.
                return joinPoint.proceed(); 
            }
            rateLimitKey = "guest:" + guestId;
        }

        // 2. Генерируем уникальный ключ для этого метода и этого пользователя/гостя
        String methodName = joinPoint.getSignature().toShortString();
        byte[] key = ("limit:" + methodName + ":" + rateLimitKey).getBytes(StandardCharsets.UTF_8);

        // 3. Получаем или создаем конфиг (кешируем по параметрам аннотации)
        String configKey = rateLimited.capacity() + "-" + rateLimited.minutes();
        BucketConfiguration config = configCache.computeIfAbsent(configKey, k -> 
            BucketConfiguration.builder()
                    .addLimit(limit -> limit.capacity(rateLimited.capacity())
                            .refillGreedy(rateLimited.capacity(), Duration.ofMinutes(rateLimited.minutes())))
                    .build());

        // 4. Проверяем лимит
        Bucket bucket = proxyManager.getProxy(key, () -> config);
        if (bucket.tryConsume(1)) {
            return joinPoint.proceed(); // Всё ок, выполняем метод
        } else {
            log.warn("Rate limit exceeded for key: {}", new String(key, StandardCharsets.UTF_8));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Лимит запросов исчерпан");
        }
    }
}
