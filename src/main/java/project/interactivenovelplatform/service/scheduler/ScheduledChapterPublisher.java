package project.interactivenovelplatform.service.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.repository.ChapterRepository;
import project.interactivenovelplatform.repository.NovelRepository;
import project.interactivenovelplatform.entity.ChapterEntity;
import project.interactivenovelplatform.entity.ChapterStatus;
import project.interactivenovelplatform.entity.NovelEntity;
import project.interactivenovelplatform.service.NotificationService;
import project.interactivenovelplatform.entity.NotificationType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScheduledChapterPublisher {
    private final ChapterRepository chapterRepository;
    private final NovelRepository novelRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void publishScheduledChapters() {
        OffsetDateTime now = OffsetDateTime.now();
        
        
        List<ChapterEntity> scheduledChapters = chapterRepository.findAllByStatusAndPublishedAtBeforeAndIsDeletedFalse(
                ChapterStatus.SCHEDULED, now
        );

        for (ChapterEntity chapter : scheduledChapters) {
            chapter.setStatus(ChapterStatus.PUBLISHED);
            chapterRepository.save(chapter);

            
            NovelEntity novel = chapter.getNovel();
            novel.setChapterCount(novel.getChapterCount() + 1);
            if (novel.getLastChapterAddedAt() == null || chapter.getPublishedAt().isAfter(novel.getLastChapterAddedAt())) {
                novel.setLastChapterAddedAt(chapter.getPublishedAt());
            }
            novelRepository.save(novel);

            // Send notification to novel followers
            notificationService.createNotificationForFollowers(
                novel.getAuthor(),
                NotificationType.NEW_CHAPTER,
                Map.of(
                    "novelId", novel.getId(),
                    "novelTitle", novel.getTitle(),
                    "chapterId", chapter.getId(),
                    "chapterTitle", chapter.getTitle(),
                    "chapterNumber", chapter.getChapterNumber()
                ),
                novel.getId()
            );
        }
    }
}

