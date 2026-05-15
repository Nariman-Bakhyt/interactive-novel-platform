package project.interactivenovelplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.repository.NovelRepository;

import java.util.concurrent.atomic.AtomicInteger;

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

        ScanOptions options = ScanOptions.scanOptions()
                .match("novel:views:buffer:*")
                .count(100)
                .build();

        // Используем AtomicInteger, так как обычный int нельзя менять внутри лямбды
        final AtomicInteger updatedCount = new AtomicInteger(0);

        redisTemplate.executeWithStickyConnection(connection -> {
            Cursor<byte[]> cursor = connection.keyCommands().scan(options);
            try (cursor) {
                while (cursor.hasNext()) {
                    String key = new String(cursor.next());
                    if (processKey(key)) {
                        updatedCount.incrementAndGet();
                    }
                }
            } catch (Exception e) {
                log.error("Ошибка при итерации ключей Redis: {}", e.getMessage());
            }
            return null;
        });

        log.info("Синхронизация завершена. Обновлено новелл: {}", updatedCount.get());
    }

    private boolean processKey(String key) {
        try {
            String[] parts = key.split(":");
            if (parts.length < 4) return false;

            Long novelId = Long.parseLong(parts[3]);

            // Атомарно забираем значение и удаляем ключ из Redis
            String value = redisTemplate.opsForValue().getAndDelete(key);

            if (value != null) {
                Long delta = Long.parseLong(value);
                if (delta > 0) {
                    novelRepository.incrementViewCount(novelId, delta);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при синхронизации ключа {}: {}", key, e.getMessage());
        }
        return false;
    }
}