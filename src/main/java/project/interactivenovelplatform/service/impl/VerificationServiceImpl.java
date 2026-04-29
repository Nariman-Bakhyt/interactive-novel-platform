package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.dto.VerificationCacheDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.entity.VerificationTokenEntity;
import project.interactivenovelplatform.entity.VerificationTokenType;
import project.interactivenovelplatform.repository.UserRepository;
import project.interactivenovelplatform.repository.VerificationRepository;
import project.interactivenovelplatform.service.EmailService;
import project.interactivenovelplatform.service.VerificationService;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {
    private final VerificationRepository tokenRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_PREFIX = "verification:";

    @Transactional
    @Override
    public void sendVerificationCode(Long userId, VerificationTokenType type, String pendingValue) {
        String lockKey = "lock:" + type + ":" + userId;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new IllegalStateException("Слишком много неудачных попыток. Доступ временно ограничен.");
        }

        AppUserEntity user = userRepository.findById(userId)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с Id: " + userId + " не найден"));
        String code = String.format("%06d", (new SecureRandom().nextInt(900000) + 100000));

        tokenRepository.deleteByUserIdAndType(user.getId(), type);
        redisTemplate.delete(getRedisKey(type, user.getId()));

        VerificationTokenEntity dbToken = new VerificationTokenEntity();
        dbToken.setToken(code);
        dbToken.setUser(user);
        dbToken.setType(type);
        dbToken.setPendingValue(pendingValue);
        dbToken.setExpiryDate(OffsetDateTime.now().plusMinutes(30));
        tokenRepository.save(dbToken);

        try {
            VerificationCacheDto cacheDto = new VerificationCacheDto(code, pendingValue);
            String jsonValue = objectMapper.writeValueAsString(cacheDto);

            redisTemplate.opsForValue().set(
                    getRedisKey(type, userId),
                    jsonValue,
                    Duration.ofMinutes(30)
            );
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при сериализации кода верификации", e);
        }

        String targetEmail = (type == VerificationTokenType.EMAIL_CHANGE) ? pendingValue : user.getEmail();
        emailService.sendCode(targetEmail, code, type);
    }

    @Transactional
    @Override
    public void verifyCode(Long userId, String code, VerificationTokenType type) {
        String lockKey = "lock:" + type + ":" + userId;
        String attemptsKey = "attempts:" + type + ":" + userId;
        String redisKey = getRedisKey(type, userId);

        // 1. Проверка блокировки
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new IllegalStateException("Слишком много неудачных попыток. Доступ временно ограничен.");
        }

        try {
            String finalPendingValue = null;
            String jsonValue = redisTemplate.opsForValue().get(redisKey);

            // 2. Ищем в Redis
            if (jsonValue != null) {
                try {
                    VerificationCacheDto cachedData = objectMapper.readValue(jsonValue, VerificationCacheDto.class);
                    if (cachedData.getCode().equals(code)) {
                        finalPendingValue = cachedData.getPendingValue();
                    }
                } catch (Exception e) {
                    // Если JSON битый, просто проигнорируем и пойдем в БД
                }
            }

            if (finalPendingValue == null) {
                VerificationTokenEntity token = tokenRepository.findByTokenAndTypeAndUser_id(code, type, userId)
                        .orElseThrow(() -> new IllegalArgumentException("Неверный код"));

                if (token.getExpiryDate().isBefore(OffsetDateTime.now())) {
                    tokenRepository.delete(token);
                    throw new IllegalArgumentException("Код просрочен");
                }
                finalPendingValue = token.getPendingValue();
            }

            applyAction(userId, type, finalPendingValue);

            tokenRepository.deleteByUserIdAndType(userId, type);
            redisTemplate.delete(List.of(redisKey, attemptsKey, lockKey));

        } catch (Exception e) {
            if (!(e instanceof IllegalStateException)) {
                handleFailedAttempt(userId, type, attemptsKey, lockKey);
            }
            throw e;
        }
    }

    private void applyAction(Long userId, VerificationTokenType type, String pendingValue) {
        switch (type) {
            case REGISTRATION_CONFIRMATION -> userRepository.activateUser(userId);
            case EMAIL_CHANGE -> userRepository.updateUserEmail(userId, pendingValue);
            case PASSWORD_RESET -> userRepository.updateUserPassword(userId, pendingValue);
            case LOGIN_BY_CODE -> {}
        }
    }

    private void handleFailedAttempt(Long userId, VerificationTokenType type, String attemptsKey, String lockKey) {
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        redisTemplate.expire(attemptsKey, Duration.ofMinutes(30));

        if (attempts != null && attempts >= 5) {
            redisTemplate.opsForValue().set(lockKey, "blocked", Duration.ofMinutes(30));
            redisTemplate.delete(attemptsKey);
        }
    }

    private String getRedisKey(VerificationTokenType type, Long userId) {
        return REDIS_KEY_PREFIX + type.name() + ":" + userId;
    }
}
