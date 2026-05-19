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



        // X-Visitor-Id (фингерпринт устройства) необходим для ограничения частоты сброса кук и предотвращения DDoS (cookie-clearing attack).
        String visitorId = request.getHeader("X-Visitor-Id");

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

            
            if (!tryConsumeGenerationLimit(visitorId)) {
                handleLimitExceeded(response);
                return;
            }

            // Подпись UUID с помощью HMAC защищает от подделки guest_id клиентом без хранения сессии на сервере (stateless).
            String newUuid = UUID.randomUUID().toString();
            String signature = generateSignature(newUuid);
            String secureToken = newUuid + "." + signature;

            Cookie guestCookie = new Cookie("guest_id", secureToken);
            guestCookie.setHttpOnly(true);
            guestCookie.setPath("/");
            guestCookie.setMaxAge(60 * 60 * 24 * 365); 

            response.addCookie(guestCookie);
            // Используем request attribute вместо SecurityContext, чтобы не загрязнять контекст безопасности неавторизованным пользователем.
            request.setAttribute("VALID_GUEST_ID", newUuid);
        } else {
            request.setAttribute("VALID_GUEST_ID", guestToken.split("\\.")[0]);
        }

        filterChain.doFilter(request, response);
    }

    private boolean tryConsumeGenerationLimit(String visitorId) {
        
        BucketConfiguration config = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(5).refillGreedy(3, Duration.ofMinutes(10)))
                .build();

        byte[] key = ("limit:gen-id:" + visitorId).getBytes(StandardCharsets.UTF_8);
        Bucket bucket = proxyManager.getProxy(key, () -> config);

        return bucket.tryConsume(1);
    }

    private void handleLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("""
            {
                "status": 429,
                "error": "Too Many Identity Changes",
                "message": "Вы слишком часто сбрасываете личность. Подождите 10 минут."
            }
            """);
    }

    private boolean validateToken(String token) {
        if (token == null || !token.contains(".")) return false;
        String[] parts = token.split("\\.");
        if (parts.length != 2) return false;
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
