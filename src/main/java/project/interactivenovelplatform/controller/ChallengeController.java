package project.interactivenovelplatform.controller;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.interactivenovelplatform.security.ChallengeService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class ChallengeController {

    private final ChallengeService challengeService;

    private final LettuceBasedProxyManager<byte[]> proxyManager;

    @Value("${app.guest.secret}")
    private String hashSecret;

    @GetMapping("/public/challenge")
    public ResponseEntity<?> getChallenge(HttpServletRequest request) {
        // 1. ПЕРВЫЙ РУБЕЖ: Ищем и валидируем куку guest_id
        Cookie[] cookies = request.getCookies();
        String guestToken = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("guest_id".equals(cookie.getName())) {
                    guestToken = cookie.getValue();
                    break;
                }
            }
        }

        // Если кука на месте и её HMAC-подпись валидна — пропускаем без генерации PoW
        if (guestToken != null && validateToken(guestToken)) {
            return ResponseEntity.ok()
                    .header("X-Skip-Challenge", "true") // Флаг для OpenResty: чувак свой
                    .build(); // Тело пустое, заголовок летит в Nginx
        }

        // =========================================================================
        // 2. ВТОРОЙ РУБЕЖ: Куки нет/невалидна. Включается логика усложнения задач и Rate Limiting
        // =========================================================================

        String frontendVisitorId = request.getHeader("X-Visitor-Id");
        boolean limitExceeded = false;

        if (frontendVisitorId != null && !frontendVisitorId.isBlank()) {
            BucketConfiguration config = BucketConfiguration.builder()
                    .addLimit(limit -> limit.capacity(30).refillGreedy(3, Duration.ofMinutes(10)))
                    .build();

            byte[] key = ("limit:gen-id:" + frontendVisitorId).getBytes(StandardCharsets.UTF_8);
            Bucket bucket = proxyManager.getProxy(key, () -> config);

            // Если бакет пуст — фиксируем, что лимит "сброса личности" исчерпан
            if (!bucket.tryConsume(1)) {
                limitExceeded = true;
            }
        }

        // Извлекаем идентификатор: приоритет X-Visitor-Id2 (из OpenResty), откатываемся на IP
        String visitorId2 = request.getHeader("X-Visitor-Id2");
        String identifier = (visitorId2 != null && !visitorId2.isBlank()) ? visitorId2 : getClientIp(request);

        // Генерируем задачу с учетом того, превысил ли бот лимиты Bucket4j
        ChallengeService.ChallengeResponse challenge = challengeService.generateChallengeWithLimit(identifier, limitExceeded);

        // Возвращаем все данные в заголовках обратно в OpenResty
        return ResponseEntity.ok()
                .header("X-Skip-Challenge", "false")
                .header("X-Challenge-Salt", challenge.salt())
                .header("X-Challenge-Difficulty", String.valueOf(challenge.difficulty()))
                .build();
    }

    @PostMapping("/public/verify-challenge")
    public ResponseEntity<?> verifyChallenge(@RequestBody Map<String, String> payload, HttpServletResponse response) {
        String salt = payload.get("salt");
        String nonce = payload.get("nonce");
        if (salt == null || nonce == null) {
            return ResponseEntity.badRequest().body("Missing salt or nonce");
        }

        boolean isValid = challengeService.verifyChallenge(salt, nonce);
        if (isValid) {
            String newUuid = UUID.randomUUID().toString();
            String signature = generateSignature(newUuid);
            String secureToken = newUuid + "." + signature;

//            Cookie guestCookie = new Cookie("guest_id", secureToken);
//            guestCookie.setHttpOnly(true);
//            guestCookie.setPath("/");
//            guestCookie.setMaxAge(60 * 60 * 24 * 365);

            ResponseCookie guestCookie = ResponseCookie.from("guest_id", secureToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(60 * 60 * 24 * 365)
                    .sameSite("Lax")
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, guestCookie.toString())
                    .body(Map.of("status", "success"));
        } else {
            return ResponseEntity.status(403).body(Map.of("status", "error", "message", "Invalid PoW or expired salt"));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
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

    private boolean validateToken(String token) {
        if (token == null || !token.contains(".")) return false;
        String[] parts = token.split("\\.");
        if (parts.length != 2) return false;
        return parts[1].equals(generateSignature(parts[0]));
    }

}