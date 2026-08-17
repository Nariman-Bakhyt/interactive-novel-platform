package project.interactivenovelplatform.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import project.interactivenovelplatform.security.UserPrincipal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final LettuceBasedProxyManager<byte[]> proxyManager;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-resources") ||
                path.startsWith("/webjars");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.contains("/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String redisKeyStr = null;
        BucketConfiguration config = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Архитектурный паттерн контроля трафика:
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            // Сценарий 1: Авторизованный пользователь — выделяем повышенный лимит для комфортного UX
            if (auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
                redisKeyStr = "limit:user:" + userPrincipal.getId();
                config = BucketConfiguration.builder()
                        .addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofMinutes(1)))
                        .build();
            }
        } else {
            // Сценарий 2: Анонимный гость — накладываем строгий лимит для защиты от парсинга контента и DDoS
            String guestId = (String) request.getAttribute("VALID_GUEST_ID");
            if (guestId != null) {
                redisKeyStr = "limit:guest:" + guestId;
                config = BucketConfiguration.builder()
                        .addLimit(limit -> limit.capacity(50).refillGreedy(30, Duration.ofMinutes(1)))
                        .build();
            }
        }

        // Если стратегия лимитирования успешно определена — выполняем списание токена из Redis
        if (redisKeyStr != null && config != null) {
            byte[] redisKey = redisKeyStr.getBytes(StandardCharsets.UTF_8);
            BucketConfiguration finalConfig = config;
            Bucket bucket = proxyManager.getProxy(redisKey, () -> finalConfig);

            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                handleLimitExceeded(response);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private void handleLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("""
            {
                "status": 429,
                "error": "Too Many Requests",
                "message": "Вы читаете слишком быстро. Подождите немного."
            }
            """);
    }
}
