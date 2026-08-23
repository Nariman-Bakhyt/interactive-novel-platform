package project.interactivenovelplatform.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GuestIdFilter extends OncePerRequestFilter {
    private final LettuceBasedProxyManager<byte[]> proxyManager;

    @Value("${app.guest.secret}")
    private String hashSecret;

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
        if (path.contains("/auth/") || path.startsWith("/ws")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        String visitorId = request.getHeader("X-Visitor-Id");
        String visitorId2 = request.getHeader("X-Visitor-Id2");

        if (visitorId == null || visitorId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"error\": \"Missing Visitor Identification\"}");
            return;
        }

        String guestToken = null;
        if (request.getCookies() != null) {
            guestToken = Arrays.stream(request.getCookies())
                    .filter(c -> "guest_id".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        if (guestToken == null || !validateToken(guestToken)) {
            // Если паспорта нет — просто отправляем на фронтенд маркер 401. Лимиты тут не
            // трогаем!
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"requires_challenge\": true}");
            return;
        } else {
            // Контур 2: Защита от обнаглевших ботов, решивших PoW (Оставляем как было, это
            // гениально)
            if (visitorId2 != null && !visitorId2.isBlank()) {
                if (!tryConsumeApiLimit(visitorId2)) {
                    handleApiLimitExceeded(response);
                    return;
                }
            }
            request.setAttribute("VALID_GUEST_ID", guestToken.split("\\.")[0]);
        }

        filterChain.doFilter(request, response);
    }

    private boolean tryConsumeApiLimit(String visitorId2) {
        BucketConfiguration config = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(120).refillGreedy(120, Duration.ofMinutes(1)))
                .build();

        byte[] key = ("limit:api:device:" + visitorId2).getBytes(StandardCharsets.UTF_8);
        Bucket bucket = proxyManager.getProxy(key, () -> config);

        return bucket.tryConsume(1);
    }

    private void handleApiLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("""
                {
                    "status": 429,
                    "error": "Too Many Requests",
                    "message": "Превышен лимит запросов к API. Подождите немного."
                }
                """);
    }

    private boolean validateToken(String token) {
        if (token == null || !token.contains("."))
            return false;
        String[] parts = token.split("\\.");
        if (parts.length != 2)
            return false;
        return parts[1].equals(generateSignature(parts[0]));
    }

    private String generateSignature(String data) {
        try {
            String algorithm = "HmacSHA256";
            SecretKeySpec secretKeySpec = new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }
}
