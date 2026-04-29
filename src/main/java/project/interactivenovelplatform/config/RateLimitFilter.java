package project.interactivenovelplatform.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final LettuceBasedProxyManager<byte[]> proxyManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String guestId = (String) request.getAttribute("VALID_GUEST_ID");

        if (guestId != null) {
            byte[] redisKey = ("limit:guest:" + guestId).getBytes(StandardCharsets.UTF_8);

            BucketConfiguration config = BucketConfiguration.builder()
                    .addLimit(limit -> limit.capacity(50).refillGreedy(50, Duration.ofMinutes(1)))
                    .build();

            Bucket bucket = proxyManager.getProxy(redisKey, () -> config);

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
