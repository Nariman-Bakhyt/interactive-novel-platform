package project.interactivenovelplatform.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final LettuceBasedProxyManager<byte[]> proxyManager;
    private final HttpServletRequest request;

    @Around("@annotation(rateLimited)")
    public Object applyRateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        // 1. Получаем ID гостя из запроса
        String guestId = (String) request.getAttribute("VALID_GUEST_ID");
        if (guestId == null) return joinPoint.proceed(); // Или кидаем ошибку

        // 2. Генерируем уникальный ключ для этого метода и этого юзера
        String methodName = joinPoint.getSignature().toShortString();
        byte[] key = ("limit:" + methodName + ":" + guestId).getBytes(StandardCharsets.UTF_8);

        // 3. Создаем конфиг из параметров аннотации
        BucketConfiguration config = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(rateLimited.capacity())
                        .refillGreedy(rateLimited.capacity(), Duration.ofMinutes(rateLimited.minutes())))
                .build();

        // 4. Проверяем лимит
        Bucket bucket = proxyManager.getProxy(key, () -> config);
        if (bucket.tryConsume(1)) {
            return joinPoint.proceed(); // Всё ок, выполняем метод
        } else {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Лимит запросов исчерпан");
        }
    }
}
