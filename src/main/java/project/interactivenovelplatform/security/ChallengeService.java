package project.interactivenovelplatform.security;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_KEY_PREFIX = "rate:challenge:global:ip:";
    private static final String SALT_KEY_PREFIX = "pow:salt:";
    private static final long RATE_TTL_SECONDS = 60;
    private static final long SALT_TTL_SECONDS = 120;

    @Value("${app.guest.secret}")
    private String hashSecret;
    /**
     * Генерирует задачу с динамической сложностью в зависимости от активности IP.
     */
    public ChallengeResponse generateChallengeWithLimit(String identifier, boolean limitExceeded) {
        String rateKey = RATE_KEY_PREFIX + identifier;
        Long requests = redisTemplate.opsForValue().increment(rateKey);

        if (requests != null && requests == 1) {
            redisTemplate.expire(rateKey, RATE_TTL_SECONDS, TimeUnit.SECONDS);
        }

        // 1. Вычисляем базовую сложность на основе счетчика в Redis
        int difficulty = calculateDifficulty(requests != null ? requests : 1);

        // 2. Если Bucket4j зафиксировал слишком частый сброс личности (X-Visitor-Id) —
        // искусственно накидываем жесткую сложность поверх базовой
        if (limitExceeded) {
            difficulty = Math.max(difficulty, 3); // На выбор: 8 или 9, чтобы бот гарантированно завис
        }

        String salt = UUID.randomUUID().toString().replace("-", "");

        // Сохраняем связку соль -> сложность в Redis для последующей верификации нонса
        redisTemplate.opsForValue().set(SALT_KEY_PREFIX + salt, String.valueOf(difficulty), SALT_TTL_SECONDS, TimeUnit.SECONDS);

        return new ChallengeResponse(salt, difficulty);
    }

    /**
     * Проверяет решение от клиента атомарно.
     */
    public boolean verifyChallenge(String salt, String nonce) {
        String saltKey = SALT_KEY_PREFIX + salt;
        
        // Получаем сложность
        String difficultyStr = redisTemplate.opsForValue().get(saltKey);
        if (difficultyStr == null) {
            return false; // Соль не найдена или истекла (или уже использована)
        }

        int difficulty = Integer.parseInt(difficultyStr);
        String targetPrefix = "0".repeat(difficulty);

        // Проверяем хэш
        String dataToHash = salt + nonce;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(dataToHash.getBytes());
            String hashHex = Hex.encodeHexString(hash);

            if (hashHex.startsWith(targetPrefix)) {
                // Успех! Атомарно удаляем соль, чтобы избежать Replay Attacks
                redisTemplate.delete(saltKey);
                return true;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }

        return false;
    }

    private int calculateDifficulty(long requestsPerMinute) {
        if (requestsPerMinute <= 100) return 2; // Базовая сложность (~15 мс)
        if (requestsPerMinute <= 500) return 3; // Подозрение на VPN (~200 мс)
        return 5; // Турбо-сложность для бота (сильно бьет по CPU)
    }

    public record ChallengeResponse(String salt, int difficulty) {}

}
