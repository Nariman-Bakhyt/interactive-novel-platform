package project.interactivenovelplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.repository.NovelRepository;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewCountSyncScheduler {
    private final RedisTemplate<String, String> redisTemplate;
    private final NovelRepository novelRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void syncViewCountsToDb() {
        log.info("Начало синхронизации просмотров из Redis в PostgreSQL...");

        Set<String> keys = redisTemplate.keys("novel:views:buffer:*");

        if (keys == null || keys.isEmpty()) {
            log.info("Буферы просмотров пусты. Синхронизация не требуется.");
            return;
        }

        int updatedCount = 0;

        for (String key : keys) {
            try {
                String[] parts = key.split(":");
                Long novelId = Long.parseLong(parts[3]);

                String value = redisTemplate.opsForValue().getAndDelete(key);

                if (value != null) {
                    Long delta = Long.parseLong(value);
                    if (delta > 0) {
                        novelRepository.incrementViewCount(novelId, delta);
                        updatedCount++;
                    }
                }
            } catch (Exception e) {
                log.error("Ошибка при синхронизации ключа {}: {}", key, e.getMessage());
            }
        }

        log.info("Синхронизация завершена. Обновлено новелл: {}", updatedCount);
    }
}

